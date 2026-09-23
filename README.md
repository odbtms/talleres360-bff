# talleres360-bff

BFF de Talleres360 (Spring Boot 3.5, Java 17). Valida el JWT de Microsoft Entra ID (issuer + audience),
autoriza por app role (Admin / Operador / Cliente) y reenvía las llamadas a ms-orders.

Los microservicios están en https://github.com/odbtms/talleres360-backend.

## Despliegue (VM ec2-bff)

```bash
git clone https://github.com/odbtms/talleres360-bff.git && cd talleres360-bff
cp .env.example .env   # completar ENTRA_TENANT_ID, API_CLIENT_ID y ORDERS_URL
docker compose up -d --build
```

## Tests

```bash
./mvnw test
```
