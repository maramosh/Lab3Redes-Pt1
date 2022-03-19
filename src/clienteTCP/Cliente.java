package clienteTCP;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.nio.file.Files;

public class Cliente {

	private static final int MESSAGE_SIZE = 1024;
	private static final int PUERTO = 21; 
	private static final String DIR_DESCARGA = "data/descargas/";
	public final static String UBICACION_LOG = "data/logs/";
	private static BufferedWriter writer;
	public final static int BUFFER_SIZE = 64000;

	// m�todo principal de la clase
	public static void main(String argv[]) {

		Scanner lectorConsola = new Scanner(System.in);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		Socket socket;

		try {
			//Se crea el log especifico para la prueba. 
			String time = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Calendar.getInstance().getTime());
			File logFile = new File(UBICACION_LOG + time + ".txt");
			writer = new BufferedWriter(new FileWriter(logFile));
			System.out.println("Escriba la direccion ip del servidor");
			String direccion = lectorConsola.next(); 
			//Se crea el socket
			socket = new Socket(direccion, PUERTO);
			socket.setReceiveBufferSize(BUFFER_SIZE);
			socket.setSendBufferSize(BUFFER_SIZE);
			System.out.println("Conectado");
			System.out.println("Esperando nombre del archivo");
			String nombreArch = "";
			DataInputStream dIn = new DataInputStream(socket.getInputStream());
			DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
			int id = 0;
			if (dIn.readByte() == 1) {
				nombreArch = dIn.readUTF();
				System.out.println("Nombre del archivo: " + nombreArch);
				writer.write("Nombre del archivo en la prueba: " + nombreArch);
				writer.newLine();
				writer.flush();
				dOut.writeByte(2);
				id = dIn.readByte();
			}
			Cliente cli = new Cliente();
			cli.descargar(socket, nombreArch, id);

		}

		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public void descargar(Socket socket, String nombreArch, int id) {
		int bytesRead = 0;
		int current;
		FileOutputStream fos;
		BufferedOutputStream bos;

		PrintWriter pw;
		BufferedReader bf;

		String inputLine;
		String outputLine;

		try {
			pw = new PrintWriter(socket.getOutputStream(), true);
			bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String timeLog = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(Calendar.getInstance().getTime());
			writer.write("Fecha y hora: " + timeLog);
			writer.newLine();
			writer.flush();

			// Recibir Hash
			DataInputStream dIn = new DataInputStream(socket.getInputStream());
			String hashRecibido = dIn.readUTF();
			writer.write("Se van a recibir paquetes de " + MESSAGE_SIZE + " bytes.");
			writer.newLine();
			writer.flush();

			// Comenzar a recibir el archivo
			byte[] mybytearray = new byte[700000000];
			InputStream is = socket.getInputStream();

			fos = new FileOutputStream(DIR_DESCARGA + nombreArch);

			bos = new BufferedOutputStream(fos);
			while (bytesRead == 0) {
				bytesRead = is.read(mybytearray, 0, mybytearray.length);
			}
			System.out.println("Recibiendo el archivo");
			current = bytesRead;

			// Iniciar medici�n tiempo descarga de un archivo.
			long startTime = System.currentTimeMillis();

			int messagesReceived = 0;
			int bytesReceived = 0;
			int numPaquetes = 0;
			do {
				bytesRead = is.read(mybytearray, current, (mybytearray.length - current));
				numPaquetes++;
				bytesReceived += bytesRead;
				if (bytesReceived >= MESSAGE_SIZE) {

					messagesReceived += (bytesReceived / MESSAGE_SIZE);
					bytesReceived -= MESSAGE_SIZE * (bytesReceived / MESSAGE_SIZE);
				}
				if (bytesRead >= 0)
					current += bytesRead;
			} while (bytesRead > -1);

			bos.write(mybytearray, 0, current);
			bos.flush();

			long endTime = System.currentTimeMillis();
			System.out.println("La descarga tomo " + (endTime - startTime) + " milisegundos");

			writer.write("El archivo se entrego, peso: (" + current + " bytes leidos)");
			writer.newLine();
			writer.flush();

			writer.write("Se recibieron " + numPaquetes + " paquetes.");
			writer.newLine();
			writer.flush();

			writer.write("La descarga tomo " + (endTime - startTime) + " milisegundos");
			writer.newLine();
			writer.flush();

			DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
			dOut.writeByte(id);

			// Verificaci�n del hash
			File myFile = new File(DIR_DESCARGA + nombreArch);
			byte[] myByteArray = Files.readAllBytes(myFile.toPath());
			byte[] hashSacado = new byte[61440];
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			hashSacado = md.digest(myByteArray);
			StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < hashSacado.length; i++) {
	          sb.append(Integer.toString((hashSacado[i] & 0xff) + 0x100, 16).substring(1));
	        }
	         
	        String hashGenerado = sb.toString();

			if (hashRecibido.equals(hashGenerado)) {
				System.out.println("Archivo verificado, todo en orden");
				writer.write("El archivo no fue modificado");
				writer.newLine();
				writer.flush();
			} else {
				System.out.println("El archivo ha sido modificado!");
				writer.write("El archivo fue modificado");
				writer.newLine();
				writer.flush();
			}

			pw.close();
			bf.close();

			fos.close();
			bos.close();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			try {
				writer.write("Hubo un error con el envio");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				System.out.println("Ocurrio un error: " + ex.getMessage());
			}
		}
	}

}