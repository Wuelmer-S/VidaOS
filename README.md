# VidaOS

App Android personal para gestionar finanzas, entrenamiento de gimnasio y mantenciones de moto en un solo lugar.

Proyecto personal construido con estándares profesionales, con doble objetivo: tener una herramienta de uso diario y aprender desarrollo Android moderno. Ver la definición completa en [`DEFINICION_PROYECTO.md`](./DEFINICION_PROYECTO.md).

## Estado actual

Fase 2 (Finanzas completo) cerrada: registro de movimientos (gasto/ingreso/pago tarjeta/interno, con origen débito/crédito/efectivo), lista con últimos 5 + historial completo, detalle y eliminación de movimientos, gráficos de gasto por categoría y por origen de pago, y gestión de categorías (crear y eliminar).

## Convenciones de desarrollo

- **CRUD por defecto:** toda funcionalidad donde el usuario crea algo (categorías, y a futuro cuotas, ejercicios, mantenciones) debe permitir también eliminarlo — ver Decisión 7 en [`DEFINICION_PROYECTO.md`](./DEFINICION_PROYECTO.md).
- **Migraciones de Room:** una vez que hay datos reales guardados, ningún cambio de esquema puede usar `fallbackToDestructiveMigration()` — ver la nota en la Decisión 4 de [`DEFINICION_PROYECTO.md`](./DEFINICION_PROYECTO.md).

## Stack

- Kotlin
- Jetpack Compose
- Arquitectura MVVM
- Room (SQLite) — se incorpora en la Fase 1

## Cómo ejecutar

1. Abrir la carpeta del proyecto en Android Studio.
2. Esperar la sincronización de Gradle.
3. Ejecutar (▶) sobre un emulador o un dispositivo físico conectado.

Por línea de comandos:

```bash
./gradlew assembleDebug
```
