# Supabase setup

Run `schema.sql` in Supabase Dashboard > SQL Editor for project `wcdahlvkddpfqgkuqzyd`.

The publishable anon key is for browser/PostgREST access and cannot create tables. This Spring Boot app uses server-side JDBC, so it needs the Supabase database password from Dashboard > Project Settings > Database.

Set these environment variables before starting the app:

```powershell
$env:SMARTCAMPUS_DB_URL = "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require"
$env:SMARTCAMPUS_DB_USER = "postgres.wcdahlvkddpfqgkuqzyd"
$env:SMARTCAMPUS_DB_PASSWORD = "<your-supabase-db-password>"
.\mvnw.cmd spring-boot:run
```

Local MySQL still works when these variables are not set.
