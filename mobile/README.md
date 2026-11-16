# VivaBem 50+ Flutter

## Rodar no Chrome

No PowerShell, dentro de `mobile`:

```powershell
flutter pub get
flutter run -d chrome --web-port=8081
```

O front usa a API em `http://localhost:8080` por padrão. A porta `8081` permite deixar o Flutter aberto ao mesmo tempo que o front web (Vite em `5173`) e já está na configuração CORS local do back. Se trocar a porta, inclua a nova origem em `CORS_ALLOWED_ORIGINS` no `.env` da raiz.

## Rodar no Android

Instale o Android Studio e o Android SDK, aceite as licenças e confirme o dispositivo com `flutter doctor` e `flutter devices`. Depois, rode no emulador:

```powershell
flutter run -d <id-do-dispositivo> --dart-define=API_URL=http://10.0.2.2:8080
```

Em um aparelho físico, troque `10.0.2.2` pelo IP local do computador que está executando a API. O emulador Android já usa esse endereço por padrão.

## Iniciar a API e o banco

Na raiz do repositório, copie `.env.example` para `.env` e defina uma senha para o PostgreSQL. Em seguida:

```powershell
docker compose up -d postgres
Set-Location backend
.\mvnw.cmd spring-boot:run
```

O back precisa de Java 17 ou superior. Para rodar o Flutter no Chrome, Chrome e Flutter SDK bastam; as telas que consultam dados precisam também da API e do banco ativos.
