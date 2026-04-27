## Decisiones técnicas

- Se utilizó H2 para simplificar la ejecución local
- Se aplicó arquitectura en capas (Controller - Service - Repository)
- Se implementaron DTOs para separar entrada y salida

- # Arquitectura Hexagonal - Módulo de Pedidos

## Estructura de paquetes

com.empresa.orders
│
├── domain
│   ├── model
│   │   └── Order
│   └── ports
│       ├── in
│       │   ├── CreateOrderUseCase
│       │   └── GetOrderUseCase
│       └── out
│           ├── OrderRepositoryPort
│           ├── NotificationPort
│           └── EventPublisherPort
│
├── application
│   └── usecase
│       └── CreateOrderService
│
├── infrastructure
│   └── adapters
│       ├── in
│       │   └── OrderController
│       └── out
│           ├── JpaOrderRepositoryAdapter
│           ├── EmailAdapter
│           └── PubSubAdapter

---

## Explicación

### Dominio

Contiene las entidades del negocio (Order) y las reglas. No depende de frameworks.

### Puertos

Son interfaces que definen la comunicación:

* Entrada: lo que el sistema expone
* Salida: lo que el sistema necesita

### Casos de uso

Se encuentran en la capa application y contienen la lógica del negocio.

### Pub/Sub

Se considera un adaptador secundario porque es un sistema externo al dominio.

---

## Ejemplo de puertos

### Entrada

public interface CreateOrderUseCase {
void createOrder(Order order);
}

public interface GetOrderUseCase {
Order getOrder(Long id);
}

### Salida

public interface OrderRepositoryPort {
Order save(Order order);
}

public interface NotificationPort {
void sendNotification(String message);
}

public interface EventPublisherPort {
void publishEvent(Order order);
}




# Consultas SQL

## A) Top 5 clientes con mayor valor de compras (últimos 30 días)

```sql
WITH recent_orders AS (
    SELECT *
    FROM orders
    WHERE created_at >= NOW() - INTERVAL '30 days'
)
SELECT 
    c.id,
    c.name,
    COUNT(o.id) AS total_orders,
    SUM(o.total) AS total_spent,
    AVG(o.total) AS avg_order
FROM customers c
JOIN recent_orders o ON c.id = o.customer_id
GROUP BY c.id, c.name
ORDER BY total_spent DESC
LIMIT 5;
```

---

## B) Productos con stock crítico y alta demanda

```sql
SELECT 
    p.name,
    p.category
FROM products p
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON o.id = oi.order_id
WHERE p.stock < 10
AND o.created_at >= NOW() - INTERVAL '30 days'
GROUP BY p.id, p.name, p.category
HAVING SUM(oi.quantity) > 50;
```

---

## C) Reporte de ventas diarias (últimos 7 días)

```sql
WITH dates AS (
    SELECT generate_series(
        NOW() - INTERVAL '6 days',
        NOW(),
        INTERVAL '1 day'
    )::date AS day
)
SELECT 
    d.day,
    p.category,
    COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS total_sales
FROM dates d
LEFT JOIN orders o ON DATE(o.created_at) = d.day
LEFT JOIN order_items oi ON o.id = oi.order_id
LEFT JOIN products p ON p.id = oi.product_id
GROUP BY d.day, p.category
ORDER BY d.day;
```

---

## Nota

Para OracleSQL:

* `NOW()` se reemplaza por `SYSDATE`
* `generate_series` puede implementarse con `CONNECT BY LEVEL`



# Uso de Redis como caché

## Estrategia

Se implementa el patrón **Cache-Aside**, donde la aplicación consulta primero la caché (Redis) y, si no encuentra el dato, lo obtiene de la base de datos y lo almacena en caché.

## Implementación en Spring Boot

```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    return repository.findById(id).orElseThrow();
}
```

```java
@CacheEvict(value = "products", key = "#id")
public Product updateProduct(Long id, Product product) {
    return repository.save(product);
}
```

---

## TTL (Time To Live)

Se define un TTL de **1 hora**, ya que los productos cambian con baja frecuencia. Esto reduce carga en la base de datos y mantiene datos relativamente actualizados.

---

## Invalidación de caché

Cada vez que un producto es actualizado o eliminado, se invalida su entrada en caché usando `@CacheEvict`.

---

## Cache Stampede

Para evitar múltiples consultas simultáneas cuando expira la caché, se puede usar:

* locking (Redis SETNX)
* o mecanismos de sincronización en Spring

---

## Beneficios

* Reducción de carga en base de datos
* Mejora de tiempos de respuesta
* Escalabilidad en picos de tráfico



# Despliegue en Google Cloud Platform

## a) Elección del servicio de cómputo

Se elige **Cloud Run** por las siguientes razones:

* Escalabilidad automática (scale to zero)
* Pago por uso (costos optimizados)
* No requiere gestión de infraestructura (serverless)
* Ideal para microservicios Spring Boot containerizados

En comparación:

* GKE requiere mayor complejidad operativa
* App Engine es menos flexible para contenedores personalizados

---

## b) Dockerfile optimizado

```dockerfile id="2dht8g"
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java","-jar","app.jar"]
```

---

## c) Manejo de secretos

Se utiliza **Google Cloud Secret Manager** para almacenar:

* credenciales de base de datos
* API keys

Los secretos se inyectan como variables de entorno en Cloud Run, evitando exponer información sensible en el código.

---

## d) Estrategia CI/CD

Se puede implementar con GitHub Actions o Cloud Build:

Pipeline:

1. Build del proyecto (Maven)
2. Ejecución de pruebas
3. Construcción de imagen Docker
4. Push a Container Registry
5. Despliegue automático a Cloud Run

Esto permite despliegues continuos al hacer merge a la rama main.



# Microservicio de Notificaciones (Node.js)

## Implementación con Strategy Pattern

```ts
interface NotificationStrategy {
  send(userId: string, message: string): void;
}

class EmailStrategy implements NotificationStrategy {
  send(userId: string, message: string) {
    console.log(`Sending EMAIL to ${userId}: ${message}`);
  }
}

class SmsStrategy implements NotificationStrategy {
  send(userId: string, message: string) {
    console.log(`Sending SMS to ${userId}: ${message}`);
  }
}

class PushStrategy implements NotificationStrategy {
  send(userId: string, message: string) {
    console.log(`Sending PUSH to ${userId}: ${message}`);
  }
}

class NotificationService {
  private strategies: Record<string, NotificationStrategy>;

  constructor() {
    this.strategies = {
      email: new EmailStrategy(),
      sms: new SmsStrategy(),
      push: new PushStrategy(),
    };
  }

  notify(userId: string, message: string, channel: string) {
    const strategy = this.strategies[channel];
    if (!strategy) {
      throw new Error("Invalid channel");
    }
    strategy.send(userId, message);
  }
}
```

---

## Endpoint (Express)

```ts
import express from "express";

const app = express();
app.use(express.json());

const service = new NotificationService();

app.post("/notify", (req, res) => {
  const { userId, message, channel } = req.body;

  service.notify(userId, message, channel);

  res.send({ status: "Notification sent" });
});

app.listen(3000, () => console.log("Server running"));
```

---

# Integración de IA

Se puede integrar Claude como agente IA para:

* Validar pedidos antes de procesarlos
* Recomendar productos
* Generar respuestas automáticas al cliente

Se utilizaría **tool use / function calling**, permitiendo que Claude invoque endpoints del backend (por ejemplo, crear pedidos o consultar productos).

También se puede implementar RAG (Retrieval-Augmented Generation) para consultar información del catálogo de productos.

La integración se realiza mediante API REST, donde el backend actúa como intermediario entre Claude y los servicios del sistema.




## Mejoras futuras
- Implementación de pruebas unitarias
- Uso de MapStruct para mapeo de DTOs
