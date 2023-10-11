package Chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientWhatsCopernic {
    public static Scanner sc = new Scanner(System.in);
    public static Socket sk;
    public static DataInputStream in;
    public static DataOutputStream out;

    public static void main(String[] args) throws IOException {
        System.out.println("¡¡Bienvenido a WhatsCopernic!! ");
        boolean salir = false;

        while (!salir) {
            salir = iniciarApp();
        }

        System.out.println("Bienvenido a WhatsCopernic");
        boolean continuar = true;

        while (continuar) {
                System.out.println("");
                System.out.println("Menú de opciones");
                System.out.println("1. Listar usuarios");
                System.out.println("2. Enviar mensaje");
                System.out.println("3. Recibir mensaje");
                System.out.println("4. Enviar archivo");
                System.out.println("5. Ver archivos");
                System.out.println("6. Recibir archivo");
                System.out.println("7. Crear grupo");
                System.out.println("8. Gestionar grupo");
                System.out.println("9. Eliminar grupo");
                System.out.println("10. Configuración");
                System.out.println("11. Salir");

                System.out.print("Elija una opción: ");
                int opcion = sc.nextInt();
                sc.nextLine();

                switch (opcion) {
                    case 1:
                        listarUsuarios();
                        break;
                    case 7:
                        crearGrupo();
                        break;
                    case 8:
                        gestionarGrupo();
                        break;
                    case 9:
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


    private static void crearGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("creargrupo " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Grupo creado con éxito");
            } else {
                System.out.println("Error al crear el grupo");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gestionarGrupo(){
        try {
            out.writeUTF("listargrupos");
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
                System.out.print("Nombre del grupo a gestionar: ");
                String nombreGrupo = sc.nextLine();
                out.writeUTF("listargrupos " + nombreGrupo);
                String response2 = in.readUTF();
                if (response2.equals("true")) {

                    System.out.println("Grupo gestionado con éxito");
                } else {
                    System.out.println("Error al gestionar el grupo");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void eliminarGrupo() {
        try {
            out.writeUTF("listargrupos");
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
                System.out.print("Nombre del grupo a eliminar: ");
                String nombreGrupo = sc.nextLine();
                out.writeUTF("eliminargrupo " + nombreGrupo);
                String response2 = in.readUTF();
                if (response2.equals("true")) {
                    System.out.println("Grupo eliminado con éxito");
                } else {
                    System.out.println("Error al eliminar el grupo");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static boolean iniciarApp() {
        try {


            System.out.println("1. Iniciar Sesión");
            System.out.println("2. Crear Cuenta");
            System.out.print("Elija una opción: ");
            int opcion = sc.nextInt();
            sc.nextLine();

            System.out.print("Usuario: ");
            String usuario = sc.nextLine();
            System.out.print("Contraseña: ");
            String password = sc.nextLine();

            sk = new Socket("localhost", 42069);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());

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
                System.out.println("Credenciales incorrectas o error al crear la cuenta.");
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

            if (response.equals("Comando incorrecto")) {
                System.out.println("Error al listar usuarios");
            } else {
                String[] usernames = response.split(", ");
                for (String username : usernames) {
                    if (!username.equals("null")) {
                        System.out.println(username);
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void logout() {
        try {
            DataOutputStream out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("logout");
            sk.close();
            System.out.println("Hasta luego!! ^^");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
