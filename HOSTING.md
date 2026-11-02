# ApexBank demo deployment

The production overlay serves the Vite app from Nginx, proxies API paths internally to the gateway, and can terminate HTTPS with Caddy. Backend services, databases, Redis, Kafka, and Eureka have no published host ports in the production overlay.

## Before deployment

1. Use a fresh Linux VPS with Docker Engine and a current Docker Compose plugin (the production overlay uses the Compose `!reset` merge tag).
2. Point a DNS A/AAAA record for your domain to the VPS and allow inbound TCP 80/443 (and UDP 443 for HTTP/3).
3. Copy `.env.example` to `.env`; replace all placeholders with unique secrets, the actual Config Server Git repository, and real SMTP credentials. Set `SITE_DOMAIN` and `SETU_CALLBACK_URL` to your HTTPS domain. Keep `.env` private.
4. Generate a persistent RSA keypair once and store it securely. Example on Linux:

   ```sh
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
   openssl pkey -in jwt-private.pem -outform DER -out jwt-private.der
   openssl pkey -in jwt-private.pem -pubout -outform DER -out jwt-public.der
   base64 -w0 jwt-private.der
   base64 -w0 jwt-public.der
   ```

   Put the outputs in `JWT_RSA_PRIVATE_KEY_BASE64` and `JWT_RSA_PUBLIC_KEY_BASE64`. Keep the private key secret and backed up. Reuse the same pair for every auth-service restart and replica.
5. Rotate any provider credentials that have ever been committed to Git; removing them from the current source does not erase repository history.
6. Take and verify backups before migrating any existing customer, KYC, or account data. A changed `POSTGRES_PASSWORD` environment value does not rotate passwords inside existing PostgreSQL volumes. Do not attach existing volumes to a deployment with new credentials until you have planned and tested that migration.

## Deploy

From the repository root on the VPS:

```sh
docker compose -f docker-compose.yml -f compose.production.yaml config --quiet
docker compose -f docker-compose.yml -f compose.production.yaml up -d --build
docker compose -f docker-compose.yml -f compose.production.yaml ps
```

Open `https://$SITE_DOMAIN`. Caddy obtains and renews the TLS certificate after DNS points to the server and ports 80/443 are reachable.

## College demo without a domain

Use the demo overlay, which publishes only the frontend at port 8088 by default and does not start Caddy. It still needs a valid `.env`: SMTP and PostgreSQL settings, a persistent RSA JWT keypair and key ID, Config Server credentials and a reachable Config Server Git repository URL, plus `SETU_CALLBACK_URL`. For a coursework demo without Setu integration, set `SETU_CALLBACK_URL` to `http://localhost:8088/api/kyc/callback`; real Setu callbacks require a publicly reachable HTTPS URL and provider credentials.

```sh
docker compose -f docker-compose.yml -f compose.production.yaml -f compose.demo.yaml config --quiet
docker compose -f docker-compose.yml -f compose.production.yaml -f compose.demo.yaml up -d --build
```

Open `http://localhost:8088` on the host. For another device on the same Wi-Fi, use `http://<host-LAN-IP>:8088` and allow that port through the host firewall. For access over the internet without a domain, install and authenticate a temporary HTTPS tunnel (for example, Cloudflare Tunnel Quick Tunnels) and point it at port 8088; the host and tunnel process must stay online for the session. This HTTP demo path is for coursework demos, not sensitive data.

To update a service, rebuild and recreate it with the same Compose files. Back up PostgreSQL volumes before schema or version changes. Never use `docker compose down -v` on a deployment with user data.

## Data and security limits

- The production override expects a new deployment to use the configured PostgreSQL password. Existing local volumes currently use their initialized credentials; setting a different environment value alone does not change those credentials.
- Current Auth signing keys are generated at service startup. Restarting auth-service invalidates outstanding JWTs; provide persistent, securely managed signing keys before requiring long-lived sessions.
- Customer-service Flyway migration `V1__unify_legacy_kyc_details.sql` keeps the legacy `kyc_details` table, fills missing auth user IDs from the existing customer link, masks legacy PAN values for the current UI, and carries the legacy verified status forward. Test it against a database snapshot and verify backups before the first deployment against important data.
- This is a college/demo banking application, not a reviewed production banking platform. Complete a security review, backup/restore exercise, and authorization/data-retention review before handling real financial or identity data.
