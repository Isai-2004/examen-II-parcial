# Examen del II parcial · Contactos con firma digital (Grupo 2 · Volley)

App Android que registra contactos con **nombre, teléfono, ubicación GPS y firma digital**,
y una **API REST** en ASP.NET Core que los guarda. La firma viaja como **image/png (máx. 1 MB)**
en una petición **multipart/form-data** hecha con **Volley**.

```
Tarea2P_Contactos/
├── api/                      API REST (C#, ASP.NET Core 8, EF Core, arquitectura limpia)
│   ├── src/Contactos.Domain          Entidad Contacto
│   ├── src/Contactos.Application     DTOs, reglas (PNG, 1 MB), servicio, interfaces
│   ├── src/Contactos.Infrastructure  EF Core + SQLite, repositorio, almacén de archivos en disco
│   ├── src/Contactos.Api             Controlador, manejo global de errores, Swagger
│   └── postman/                      Colección de Postman + archivos de prueba
├── android/                  App Android (Java, Activities, Volley)
└── INICIAR_API.bat           Inicia la API con doble clic
```

## 1. Ejecutar la API

Requisito: .NET 8 SDK.

```bash
cd api/src/Contactos.Api
dotnet run
```

o doble clic en **INICIAR_API.bat**. La primera vez crea la base `contactos.db` y la carpeta `almacen/firmas`.

- Swagger: http://localhost:5080/swagger
- Desde el emulador Android la API se ve como `http://10.0.2.2:5080/`.

### Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/contactos` | multipart/form-data: `nombre`, `telefono`, `latitud`, `longitud`, `archivo` | 201, 400, 415 |
| GET | `/api/contactos?buscar=` | Lista (filtra por nombre o teléfono) | 200 |
| GET | `/api/contactos/{id}` | Un contacto | 200, 404 |
| GET | `/api/contactos/{id}/archivo` | Descarga la firma con `Content-Type: image/png` | 200, 404 |
| PUT | `/api/contactos/{id}` | JSON: `nombre`, `telefono`, `latitud`, `longitud` | 200, 400, 404 |
| DELETE | `/api/contactos/{id}` | Elimina el contacto y su archivo | 204, 404 |

### Reglas del archivo

- Se guarda en disco (`almacen/firmas/<guid>.png`); la base de datos guarda solo **ruta, MIME y tamaño**.
- Mayor a 1 MB → **400**. Tipo distinto de `image/png` → **415**. Además se comprueba que el contenido sea un PNG real (firma de bytes `89 50 4E 47`).
- Todos los errores responden JSON (ProblemDetails). La app muestra `detail` y los mensajes de `errors`:

```json
{
  "title": "Tipo de archivo no permitido",
  "status": 415,
  "detail": "Tipo de archivo no permitido (image/jpeg). Solo se acepta image/png."
}
```

### Postman

Importar `api/postman/Contactos.postman_collection.json`. En **Settings → General → Working directory**
seleccionar la carpeta `api/postman` (ahí están `firma_ejemplo.png`, `no_es_png.jpg` y `firma_grande.png`).
Ejecutar la colección en orden: prueba 201, 400, 415, 400 (1 MB), listar, buscar, descargar, actualizar, 400, eliminar y 404.

## 2. Ejecutar la app Android

1. Android Studio → **Open** → carpeta `android`.
2. Iniciar un emulador con Google Play y presionar **Run**.
3. Con la API corriendo, la app ya se conecta a `http://10.0.2.2:5080/`.

**Mapa (opcional):** para ver el mapa dentro de la app, agregar en `android/local.properties`:

```
MAPS_API_KEY=tu_clave_de_google_maps
```

Sin clave, al elegir "Sí" en el diálogo la ubicación se abre en la app de Google Maps.

**Ubicación en el emulador:** ⋮ (Extended controls) → Location → escribir latitud/longitud → *Set location*.

### Dónde está cada requisito

| Requisito | Archivo |
|---|---|
| RequestQueue única (singleton) | `red/VolleySingleton.java` |
| Multipart sin Base64 (`getBodyContentType`, `getBody`, boundary) | `red/VolleyMultipartRequest.java` |
| Descarga con `Request<byte[]>` | `red/ByteArrayRequest.java` |
| JsonArrayRequest (GET), JsonObjectRequest (PUT), StringRequest (DELETE), DefaultRetryPolicy | `red/ApiContactos.java` |
| Mensaje JSON de la API y error de conectividad con Reintentar | `red/ErrorApi.java`, `ui/Dialogos.java` |
| Firma con Canvas y Path, exportar PNG, validar que no esté vacía | `ui/FirmaView.java` |
| FusedLocationProviderClient, GPS apagado y permiso denegado | `ui/MainActivity.java` |
| Lista, búsqueda, firma descargada, actualizar, eliminar, llamar | `ui/ListaContactosActivity.java` |
| Mapa con marcador | `ui/MapaActivity.java` |
| Indicador de carga en cada petición | `ui/Dialogos.java` (Cargando) y barra de progreso de la lista |

Ninguna petición se ejecuta en el hilo principal: Volley las procesa en sus hilos y entrega la respuesta en el hilo principal.
