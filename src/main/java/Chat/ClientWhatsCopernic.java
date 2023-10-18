package Chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Properties;

public class ClientWhatsCopernic {
    public static Scanner sc = new Scanner(System.in);
    public static Socket sk;
    public static DataInputStream in;
    public static DataOutputStream out;
    private static ClientConfiguration clientConfig;

    public static void main(String[] args) {
        loadClientConfiguration();
        System.out.println("¡¡Bienvenido a WhatsCopernic!!");
        boolean salir = false;

        while (!salir) {
            salir = iniciarApp();
        }

        System.out.println("Bienvenido a WhatsCopernic");
        boolean continuar = true;

        while (continuar) {
            System.out.println();
            System.out.println("Menú de opciones");
            System.out.println("1. Listar usuarios");
            System.out.println("2. Listar grupos");
            System.out.println("3. Enviar mensaje");
            System.out.println("4. Recibir mensaje");
            System.out.println("5. Enviar archivo");
            System.out.println("6. Ver archivos");
            System.out.println("7. Recibir archivo");
            System.out.println("8. Crear grupo");
            System.out.println("9. Gestionar grupo");
            System.out.println("10. Eliminar grupo");
            System.out.println("11. Salir");
            int opcion = verificarInput(sc);
            sc.nextLine();

            switch (opcion) {
                case 1:
                    listarUsuarios();
                    break;
                case 2:
                    listarGrupos();
                case 3:
                    enviarMensaje();
                    break;
                case 4:
                    listarMensaje();
                    break;
                case 5:
                    enviarArchivo();
                    break;
                case 6:
                    listarArchivos();
                    break;
                case 7:
                    recibirArchivo();
                    break;
                case 8:
                    crearGrupo();
                    break;
                case 9:
                    listarGrupos();
                    gestionarGrupo();
                    break;
                case 10:
                    listarGrupos();
                    eliminarGrupo();
                    break;
                case 11:
                    logout();
                    continuar = false;
                    break;
                default:
                    System.out.println("Opción inválida");
                    break;
            }
        }
    }

    public static boolean iniciarApp() {
        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            out = new DataOutputStream(sk.getOutputStream());
            in = new DataInputStream(sk.getInputStream());

            System.out.println("1. Iniciar Sesión");
            System.out.println("2. Crear Cuenta");
            System.out.print("Elija una opción: ");
            int opcion = verificarInput(sc);
            sc.nextLine();

            System.out.print("Usuario: ");
            String usuario = sc.nextLine();
            System.out.print("Contraseña: ");
            String password = sc.nextLine();

            if (opcion == 1) {
                out.writeUTF("login " + usuario + " " + password);
            } else if (opcion == 2) {
                out.writeUTF("create " + usuario + " " + password);
            } else {
                System.out.println("Opción inválida");
                return false;
            }
            String respuesta = in.readUTF();
            if (respuesta.equals("true")) {
                System.out.println();
                System.out.println("Sesión iniciada.");
                return true;
            } else {
                System.out.println(respuesta);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void listarUsuarios() {
        try {
            out.writeUTF("listar");
            String response = in.readUTF();
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        esperar(2000);
    }

    public static void enviarMensaje() {
        System.out.println("1. Mensaje a un usuario");
        System.out.println("2. Mensaje a un grupo");
        int opcion = verificarInput(sc);
        sc.nextLine();
        switch (opcion) {
            case 1:
                mensajeUsuario();
                break;
            case 2:
                mensajeGrupo();
                break;
            default:
                System.out.println("Opción inválida");
                break;
        }
    }


    public static void mensajeUsuario() {
        try {
            System.out.print("Ingrese el nombre del destinatario: ");
            String destinatario = sc.nextLine();
            System.out.print("Ingrese el mensaje: ");
            String mensaje = sc.nextLine();

            out.writeUTF("mensaje " + destinatario + " " + mensaje);

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);
            esperar(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void mensajeGrupo() {
        try {
            System.out.print("Ingrese el nombre del grupo: ");
            String destinatario = sc.nextLine();
            System.out.print("Ingrese el mensaje: ");
            String mensaje = sc.nextLine();

            out.writeUTF("mensajeGrupo " + destinatario + " " + mensaje);

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);
            esperar(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void listarMensaje() {
        System.out.println("1. Mensaje de usuario");
        System.out.println("2. Mensaje de grupo");
        int opcion = verificarInput(sc);
        sc.nextLine();
        switch (opcion) {
            case 1:
                listarUsuarios();
                listarMensajesUsuario();
                break;
            case 2:
                listarGrupos();
                listarMensajesGrupo();
                break;
            default:
                System.out.println("Opción inválida");
                break;
        }
        esperar(2000);
    }

    public static void listarMensajesUsuario() {
        try {
            System.out.print("Recibir mensajes de: ");
            String destinatario = sc.nextLine();

            out.writeUTF("listarmensajes " + destinatario);

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);

        } catch (IOException e) {
            e.printStackTrace();
        }
        esperar(2000);
    }

    public static void listarMensajesGrupo() {
        try {
            System.out.print("Recibir mensajes de: ");
            String destinatario = sc.nextLine();

            out.writeUTF("listarmensajesgrupo " + destinatario);

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);
            esperar(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void enviarArchivo() {
        System.out.println("1. Enviar archivo a un usuario");
        System.out.println("2. Enviar archivo a un grupo");
        int opcion = verificarInput(sc);
        sc.nextLine();

        switch (opcion) {
            case 1:
                listarUsuarios();
                enviarArchivoUsuario();
                break;
            case 2:
                listarGrupos();
                enviarArchivoGrupo();
                break;
            default:
                System.out.println("Opción inválida");
                break;
        }
    }


    private static void enviarArchivoUsuario() {
        try {
            System.out.println("Enviar a: ");
            System.out.println("0. Todo el mundo");
            System.out.println("1. Solo un usuario");
            int permisos = verificarInput(sc);
            sc.nextLine();
            if (permisos == 0) {
                System.out.print("Ruta del archivo: ");
                String rutaArchivo = sc.nextLine();
                File file = new File(rutaArchivo);
                if (file.length() > clientConfig.tamanoMaximoArchivo) {
                    System.out.println("El archivo es demasiado grande");
                    return;
                }
                out.writeUTF("enviararchivotodos " + rutaArchivo + " " + permisos);
                String response = in.readUTF();
                if (response.equals("true")) {
                    System.out.println("Archivo enviado con éxito");
                } else {
                    System.out.println("Error al enviar el archivo");
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (permisos == 1) {
                System.out.print("Nombre del usuario: ");
                String nombreUsuario = sc.nextLine();
                System.out.print("Ruta del archivo: ");
                String rutaArchivo = sc.nextLine();
                File file = new File(rutaArchivo);
                if (file.length() > clientConfig.tamanoMaximoArchivo) {
                    System.out.println("El archivo es demasiado grande");
                    return;
                }
                out.writeUTF("enviararchivousuario " + nombreUsuario + " " + rutaArchivo + " " + permisos);
                String response = in.readUTF();
                if (response.equals("true")) {
                    System.out.println("Archivo enviado a " + nombreUsuario + " con éxito");
                } else {
                    System.out.println("Error al enviar el archivo");
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Opción inválida");
            }
        } catch (IOException e) {
            System.out.println("Error al enviar el archivo");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void enviarArchivoGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            System.out.print("Ruta del archivo: ");
            String rutaArchivo = sc.nextLine();
            File file = new File(rutaArchivo);
            if (file.length() > clientConfig.tamanoMaximoArchivo) {
                System.out.println("El archivo es demasiado grande");
                return;
            }
            out.writeUTF("enviararchivogrupo " + nombreGrupo + " " + rutaArchivo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Archivo enviado al grupo " + nombreGrupo + " con éxito");
            } else {
                System.out.println("Error al enviar el archivo");
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Error al enviar el archivo");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public static void listarArchivos() {
        try {
            out.writeUTF("listararchivos");
            String response = in.readUTF();
            if (response.equals("Comando incorrecto")) {
                System.out.println("Error al listar archivos");
            } else {
                String[] archivos = response.split(", ");
                for (String archivo : archivos) {
                    if (!archivo.equals("null")) {
                        System.out.println(archivo);
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static void recibirArchivo() {
        try {
            System.out.print("Nombre del archivo: ");
            String archivo = sc.nextLine();
            out.writeUTF("recibirarchivo " + archivo);

            String serverResponse = in.readUTF();

            if (serverResponse.equals("Archivo")) {
                DataInputStream fileIn = new DataInputStream(sk.getInputStream());
                String fileName = fileIn.readUTF();
                long fileSize = fileIn.readLong();

                File downloadFolder = new File(clientConfig.rutaDescargaArchivos);
                if (!downloadFolder.exists()) {
                    downloadFolder.mkdirs(); // Crea la carpeta si no existe
                }

                FileOutputStream fileOut = new FileOutputStream(downloadFolder + File.separator + fileName);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while (fileSize > 0 && (bytesRead = fileIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }

                fileOut.close();
                System.out.println(archivo + " recibido con éxito");
            } else if (serverResponse.equals("Archivo no encontrado")) {
                System.out.println("Archivo no encontrado en el servidor");
            } else {
                System.out.println("Respuesta inesperada del servidor: " + serverResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al recibir el archivo: " + e.getMessage());
        }
    }


    private static void crearGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("creargrupo " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Grupo creado con éxito");
                esperar(2000);
            } else {
                System.out.println("Error al crear el grupo");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void listarGrupos() {
        try {
            out.writeUTF("listargrupos ");
            String response = in.readUTF();
            if (response.equals("Comando incorrecto")) {
                System.out.println("Error al listar grupos");
            } else {
                String[] grupos = response.split(", ");
                for (String grupo : grupos) {
                    if (!grupo.equals("null")) {
                        System.out.println(grupo);
                    }
                }
                esperar(2000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gestionarGrupo() {

        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("vermiembros " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("Comando incorrecto")) {
                System.out.println("Error al listar miembros");
            } else {
                String[] miembros = response.split(", ");
                for (String miembro : miembros) {
                    if (!miembro.equals("null")) {
                        System.out.println(miembro);
                    }
                }
                esperar(2000);
            }

            boolean esAdmin = verificarSiEsAdmin(nombreGrupo);

            if (esAdmin) {
                System.out.println("1. Agregar usuario a un grupo");
                System.out.println("2. Eliminar usuario de un grupo");
                System.out.println("3. Dar/quitar permisos");
                int opcion = verificarInput(sc);
                sc.nextLine();
                switch (opcion) {
                    case 1:
                        agregarUsuarioGrupo();
                        break;
                    case 2:
                        eliminarUsuarioGrupo();
                        break;
                    case 3:
                        darQuitarPermisos();
                        break;
                    default:
                        System.out.println("Opción inválida");
                        break;
                }
            } else {
                System.out.println("No eres administrador del grupo");
                esperar(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean verificarSiEsAdmin(String nombreGrupo) {
        try {
            out.writeUTF("veradmin " + nombreGrupo);
            String response = in.readUTF();
            return response.equals("true");
        } catch (Exception e) {
            System.out.println("Error al verificar si es admin");
            esperar(2000);
            return false;
        }
    }

    private static void agregarUsuarioGrupo() {
        try {
            System.out.print("Nombre del usuario: ");
            String nombreUsuario = sc.nextLine();
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("agregarusuario " + nombreUsuario + " " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Usuario agregado con éxito");
            } else {
                System.out.println("Error al agregar el usuario");
            }
            esperar(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void eliminarUsuarioGrupo() {
        try {
            System.out.print("Nombre del usuario: ");
            String nombreUsuario = sc.nextLine();
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("eliminarusuario " + nombreUsuario + " " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Usuario eliminado con éxito");
            } else {
                System.out.println("Error al eliminar el usuario");
            }
            esperar(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void darQuitarPermisos() {
        try {
            System.out.print("Nombre del usuario: ");
            String nombreUsuario = sc.nextLine();
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            System.out.println("1. Dar permisos");
            System.out.println("2. Quitar permisos");
            int opcion = verificarInput(sc);
            sc.nextLine();
            if (opcion == 1) {
                out.writeUTF("darpermisos " + nombreUsuario + " " + nombreGrupo);
            } else if (opcion == 2) {
                out.writeUTF("quitarpermisos " + nombreUsuario + " " + nombreGrupo);
            } else {
                System.out.println("Opción inválida");
            }
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Permisos modificados con éxito");
            } else {
                System.out.println("Error al modificar los permisos");
            }
            esperar(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void eliminarGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("eliminargrupo " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Grupo eliminado con éxito");
            } else {
                System.out.println(response);
            }
            esperar(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logout() {
        try {
            out.writeUTF("logout");
            sk.close();
            System.out.println("Hasta luego!! ^^");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int verificarInput(Scanner sc) {
        System.out.print("Elije una opción: ");
        int opcion;
        while (true) {
            if (sc.hasNextInt()) {
                opcion = sc.nextInt();
                break;
            } else {
                System.out.print("Opción no válida. \nElije una opción:  ");
                sc.nextLine();
            }
        }
        return opcion;
    }

    public static void esperar(int tiempo) {
        try {
            Thread.sleep(tiempo);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class ClientConfiguration {
        public int tamanoMaximoArchivo;
        public String rutaDescargaArchivos;
        public String ipServidor;
        public int portServidor;

        public ClientConfiguration(int tamanoMaximoArchivo, String rutaDescargaArchivos, String ipServidor, int portServidor) {
            this.tamanoMaximoArchivo = tamanoMaximoArchivo;
            this.rutaDescargaArchivos = rutaDescargaArchivos;
            this.ipServidor = ipServidor;
            this.portServidor = portServidor;
        }
    }

    private static void loadClientConfiguration() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("client.properties")) {
            properties.load(fis);

            // Carga las variables del archivo de configuración
            clientConfig = new ClientConfiguration(
                    Integer.parseInt(properties.getProperty("tamanoMaximoArchivo")),
                    properties.getProperty("rutaDescargaArchivos"),
                    properties.getProperty("ipServidor"),
                    Integer.parseInt(properties.getProperty("portServidor"))
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}