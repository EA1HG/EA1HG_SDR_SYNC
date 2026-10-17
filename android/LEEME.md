# EA1HG SDR — app Android (control remoto)

Una app Android que abre la página de control remoto de EA1HG SDR SYNC en una ventana propia: guarda la dirección y la
contraseña, sigue sonando con la pantalla apagada y no necesita el navegador.

## Cómo conseguir el APK (sin instalar nada en tu PC)

1. Sube estos archivos al repositorio de GitHub (la carpeta `android/` y `.github/workflows/android-apk.yml`).
2. En GitHub: pestaña **Actions → Android APK → Run workflow**. Tarda unos 3–5 minutos.
3. Cuando termine, entra en esa ejecución y descarga **EA1HG_SDR_remoto** (un zip con `EA1HG_SDR_remoto.apk`).
4. Pasa el APK al móvil e instálalo (Android pedirá permiso para instalar apps de fuentes desconocidas).

Para que además se adjunte a una Release: crea una etiqueta que empiece por `android-v`, por ejemplo `android-v1.0.0`.

## Primer uso

- Al abrirla pide la **dirección** (la misma que usabas en el navegador, p. ej. `http://100.x.x.x:8765`) y la **contraseña**
  del control remoto. Se guardan en el móvil.
- El botón **⚙** de la página abre los ajustes otra vez (cambiar dirección, contraseña, mantener pantalla encendida, salir).
- Permite las notificaciones: la app muestra un aviso fijo «Conectado con el PC» que evita que Android la duerma. Desde ese
  aviso se puede **Salir**.
- El botón Atrás no cierra la app (el audio sigue); para cerrarla del todo usa «Salir».

## Notas

- Es un APK de **depuración** (firmado con la clave de depuración): vale para instalarlo tú, no para Google Play.
- La contraseña se guarda en el almacenamiento privado de la app, sin cifrado adicional.
- Necesita Android 8 o superior.
