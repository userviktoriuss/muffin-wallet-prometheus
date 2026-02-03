podman run -d \
  --name postgres-wallet \
  -e POSTGRES_DB=muffin_wallet \
  -e POSTGRES_USER=muffin-wallet \
  -e POSTGRES_PASSWORD=muffin-wallet \
  -p 5432:5432 \
  postgres;