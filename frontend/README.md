# OrderFlow Frontend

React + Vite console for the OrderFlow demo.

It can:

- Issue a demo JWT from the API Gateway
- Check inventory through `/api/inventory/{id}`
- Place an order through `/api/orders`
- Refresh order status after Kafka updates the order

Run it with:

```powershell
npm install
npm run dev
```

Set the gateway URL if needed:

```powershell
$env:VITE_API_URL="http://localhost:8080"
npm run dev
```
