//package progetto_laboratorio_di_reti;

/**** import ****/

import java.util.Scanner;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.net.*;
import java.net.UnknownHostException;
import java.io.*;

/**** code  ****/

/**
 * Implementation of the client of Wordle
 * 
 * @author Andrea Lepori
 * @version 1.0
 *
 */

public class WordleClient {

	/**
	 * string with multicast address
	 */
	private String multicastAddress;

	/**
	 * InetAddress del gruppo di multicast
	 */
	private InetAddress welcomeGroup;

	/**
	 * timeout socket tcp espresso in millisecondi
	 */
	private int timeout_socket_tcp;

	/**
	 * porta associata al gruppo multicast
	 */
	private int port_multicast;

	/**
	 * porta oggetto remoto callback
	 */
	private int port_callbackobj;

	/**
	 * porta associata al registry
	 */
	private int port_registry;

	/**
	 * porta associata al socket tcp
	 */
	private int port_socket_tcp;

	/**
	 * multicast socket
	 */
	private MulticastSocket multicastWelcome;

	/**
	 * list of messages from multicast group
	 */
	private List<String> multicastMessage;

	/**
	 * classe interna che implementa il task che iscrive il client al gruppo di
	 * multicast e sta in ascolto sul gruppo di multicast per ricevere i messaggi
	 * 
	 */
	public class MulticastManager implements Runnable {

		// metodo costruttore
		@SuppressWarnings("deprecation")
		public MulticastManager() {

			// inizializzo la struttura che conterrà i messaggi provenienti dal gruppo di
			// multicast
			// utilizzo la classe Vector xk è una struttura sincronizzata
			// dato che questa risorsa sarà condivisa dal thread per la gestione del
			// multicast e dal thread main
			multicastMessage = new Vector<String>();
			try {
				// Determines the IP address of the Multicast group, given the host's name.
				welcomeGroup = InetAddress.getByName(multicastAddress);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			// verifica che l'indirizzo passato come argomento sia valido per il multicast
			if (!welcomeGroup.isMulticastAddress()) {
				throw new IllegalArgumentException("indirizzo non valido come indirizzo di multicast");
			}

			// unirsi al gruppo di multicast
			try {
				// Constructs a multicast socket and binds it to the specified port on the local
				// host machine. The socket will be bound to the wildcard address.
				multicastWelcome = new MulticastSocket(port_multicast);
				// Joins a multicast group. InetAddress welcomeGroup is the multicast address to
				// join
				multicastWelcome.joinGroup(welcomeGroup);
				System.out.println("Unito al gruppo di multicast");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// @override, metodo attraverso cui il client sta in ascolto sul gruppo di
		// multicast
		public void run() {
			while (true) {
				try {
					Thread.sleep(60000); // dormo per un minuto
					// preparo un array di byte per contenere i dati in arrivo
					byte[] buf = new byte[2048];
					// preparo il pacchetto UDP per fare la ricezione
					// Constructs a DatagramPacket for receiving packets of length length.
					// The length argument must be less than or equal to buf.length.

					DatagramPacket dp = new DatagramPacket(buf, buf.length);

					// vado a leggere dal multicast Socket
					// This method blocks until a datagram is received
					multicastWelcome.receive(dp);

					// converto i byte ricevuti in una stringa
					// Constructs a new String by decoding the specified array of bytes using the
					// specified charset.
					// Charset <--> A named mapping between sequences of sixteen-bit Unicode code
					// units and sequences of bytes.

					String str = new String(buf, "ASCII");

					// System.out.println(str.trim());

					// aggiungo quello che ho letto alla lista
					// che memorizza i msg provenienti dal gruppo multicast
					multicastMessage.add(str);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) { // perchè sleep è bloccante
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * metodo costruttore della classe WordleClient, settaggio dei parametri di
	 * configurazione
	 */
	public WordleClient(String multicastAddress, int port_multicast, int port_registry, int port_socket_tcp,
			int port_callbackobj, int timeout_socket_tcp) {
		this.multicastAddress = multicastAddress;
		this.port_multicast = port_multicast;
		this.port_registry = port_registry;
		this.port_socket_tcp = port_socket_tcp;
		this.port_callbackobj = port_callbackobj;
		this.timeout_socket_tcp = timeout_socket_tcp;
	}

	// metodo per visualizzare le condivisioni inviate dagli utenti sul gruppo di
	// multicast e memorizzate nella struttura multicastMessage che è una
	// Vector<String>
	private void showMesharing() {
		if (multicastMessage.isEmpty()) { // la struttura non contiene elem
			System.out.println("There are no sharings availables");
			return;
		}
		while (!multicastMessage.isEmpty()) { // la struttura contiene almeno un elem
			// stampo il primo elem della lista
			System.out.println(multicastMessage.get(0).trim());
			// lo rimuovo
			multicastMessage.remove(0);
		}

	}

	// metodo per chiedere di condividere i suggerimenti sul gruppo di multicast
	private boolean share(String username, String password, DataOutputStream dos, BufferedReader reader) {
		// messaggio da inviare al server secondo protocollo
		// username/password/cmd in questo caso cmd == share
		String tosend = username + "/" + password + "/" + "share";
		try {
			// invio la lunghezza del msg da spedire
			dos.writeInt(tosend.length());
			// invio il msg
			// Writes out the string to the underlying output stream as asequence of bytes.
			// Each character in the string is written out, insequence, by discarding its
			// high eight bits.
			dos.writeBytes(tosend);
			// Flushes this data output stream. This forces any buffered outputbytes to be
			// written out to the stream.
			dos.flush();

			// recupera la linea che il server invia come risposta
			// Reads a line of text. A line is considered to be terminated by any oneof a
			// line feed ('\n'), a carriage return ('\r'), a carriage returnfollowed
			// immediately by a line feed, or by reaching the end-of-file(EOF).
			// Returns:A String containing the contents of the line, not includingany
			// line-termination characters, or null if the end of thestream has been reached
			// without reading any characters

			// ora devo leggere la risposta del server
			// da protocollo il server invia linee, ovvero stringhe terminate da '/n'
			String rispostaserver = reader.readLine();

			if (rispostaserver == null) {
				System.out.println("Message null");

				return false;

			}
			// System.out.println(rispostaserver);
			// tokenizzo la stringa in risposta usando '/' come separatore
			StringTokenizer tokenizedLine = new StringTokenizer(rispostaserver, "/");
			if (!tokenizedLine.nextToken().equals(username)) {
				return false; // il server mi rimanda indietro il mio username

			}
			if (!tokenizedLine.nextToken().equals("share")) {
				return false; // il sever mi risponde con lo stesso comando

			}
			if (tokenizedLine.nextToken().equals("ok")) {
				return true; // ok se l'operazione di sharing è andata a buon fine
				// ovvero è stato effettuato l'invio del datagramma sul multicast group
			} else
				return false; // no se l'invio sul gruppo non è andato a buon fine
		}

		catch (IOException exc) {
			return false;
		}

	}

	// metodo che implementa la richiesta di logout dell' utente con le credenziali
	// passate in input
	// restituisce true se e solo se il logout è andato a buon fine, false
	// altrimenti
	private boolean logout(String username, String password, BufferedReader reader, DataOutputStream dos) {

		String tosend = username + "/" + password + "/" + "logout";
		try {

			dos.writeInt(tosend.length());
			dos.writeBytes(tosend);
			dos.flush();

			// recupera la prima linea della risposta del server
			String rispostaserver = reader.readLine();
			// System.out.println(rispostaserver);
			if (rispostaserver == null) {
				System.out.println("Message null");

				return false;

			}
			// System.out.println(rispostaserver);

			StringTokenizer tokenizedLine = new StringTokenizer(rispostaserver, "/");
			if (!tokenizedLine.nextToken().equals(username)) {
				return false;

			}
			if (!tokenizedLine.nextToken().equals("logout")) {
				return false;

			}
			if (tokenizedLine.nextToken().equals("ok")) {
				return true;
			} else
				return false;
		}

		catch (IOException exc) {
			return false;
		}

	}

	/**
	 * metodo per eseguire il login attraverso la chiamata di metodi remoti
	 * 
	 * @param username inserito dall' utente che vuole loggarsi
	 * @param password inserito dall' utente che vuole loggarsi
	 * @param stub     del servizio che gestisce il database delle registrazioni
	 * @return true se e solo se il login è andato a buon fine, false altrimenti
	 */
	private boolean login(String username, String password, RegistrationService stub) {
		try {
			if (!stub.isRegistered(username)) { // controllo che sia registrato un utente con quell' username
				System.out.println("Per effettuare login devi prima registrarti");
				System.out.println("Per registrarti digita registra");
				return false;
			}
			// controllo che la password inserita sia uguale a quella nel database
			if (password.equals(stub.getPassword(username))) {
				// eseguo il login invocando il metodo remoto opportuno
				stub.setLogged(username, password);
				return true;
			} else {
				System.out.println("password sbagliata");
				return false;
			}

		} catch (Exception e) {
			System.out.println("Error in invoking object method");
			e.printStackTrace();
			return false;
		}
	}

	// metodo che va a registrare un utente con le credenziali username e password
	// passate in input nel db delle registrazioni attraverso l'utilizzo di metodi
	// remoti
	// return true se e solo se la registrazione è andata a buon fine, false
	// altrimenti
	private boolean register(String username, String password, RegistrationService stub) {
		try {
			if (stub.addRegistration(username, password))
				return true;
			else
				return false;
		} catch (Exception e) {
			System.out.println("Error in invoking object method");
			return false;
		}
	}

	// attraverso questo metodo l' utente che vuole iniziare una partita a wordle
	// lo comunica al server
	// restituisce true se e solo se la partita è stata avviata correttamente
	private boolean playWordle(String username, String password, DataOutputStream dos, BufferedReader reader) {
		// preparo la stringa da mandare al server e la invio con una write
		String tosend = username + "/" + password + "/" + "gioca\n";
		// la mando
		try {
			dos.writeInt(tosend.length());
			// System.out.print(tosend.length());
			dos.writeBytes(tosend);
			dos.flush();
			//System.out.print(tosend);

			// aspetto la risposta del server

			// recupera la linea di risposta del server
			String rispostaserver = reader.readLine();
			if (rispostaserver == null) {
				System.out.println("Message null");
				return false;
			}
			// System.out.println(rispostaserver);

			StringTokenizer tokenizedLine = new StringTokenizer(rispostaserver, "/");
			if (!tokenizedLine.nextToken().equals(username))
				return false;
			if (!tokenizedLine.nextToken().equals("gioca"))
				return false;
			if (tokenizedLine.nextToken().equals("ok")) {
				return true;
			} else
				return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * metodo che gestisce l'invio di una guessedword al server
	 * 
	 * @param username    dell' utente che sta inviando la parola
	 * @param password    dell' utente che sta inviando la parola
	 * @param guessedWord parola inviata
	 * @param reader
	 * @param dos
	 */
	private void sendWord(String username, String password, String guessedWord, BufferedReader reader,
			DataOutputStream dos) {
		// Stringa che conterrà i suggerimenti
		String tips = null;
		// preparo la stringa da mandare al server e la invio con una write
		String tosend = username + "/" + password + "/" + "guess" + "/" + guessedWord + "\n";
		// System.out.println(tosend);
		// la mando
		try {
			dos.writeInt(tosend.length());
			// System.out.print(tosend.length());

			dos.writeBytes(tosend);
			dos.flush();

			// recupera la prima linea della risposta del server
			String rispostaserver = reader.readLine();
			// System.out.println(rispostaserver);
			if (rispostaserver == null)
				System.out.println("Message null");
			StringTokenizer tokenizedLine = new StringTokenizer(rispostaserver, "/");
			if (tokenizedLine.nextToken().equals(username)) {
				if (tokenizedLine.nextToken().equals("guess")) {
					String status = tokenizedLine.nextToken();
					if (status.equals("ok")) { // esito della risposta positivo
						// parola indovinata
						// la stringa dei suggerimenti sarà ++++++++++
						tips = tokenizedLine.nextToken();
						// prendo la traduzione della parola segreta
						String traduzione = tokenizedLine.nextToken();
						System.out.println("Stringa dei suggerimenti: " + tips);
						System.out.println(
								"parola indovinata, la traduzione italiana della parola segreta e': " + traduzione);
					} else if (status.equals("ritenta")) {
						tips = tokenizedLine.nextToken();
						System.out.println("Stringa dei suggerimenti: " + tips);
						System.out.println("parola errata, ritenta");
						System.out.println("Per inviare la parola myword digita: send myword");
					} else if (status.equals("fine")) {
						tips = tokenizedLine.nextToken();
						if(!tips.equals("nada"))
						System.out.println(tips);
						String traduzione = tokenizedLine.nextToken();
						System.out.println(
								"parola errata, hai esaurito i tentativi, la traduzione italiana della parola segreta e': "
										+ traduzione);
					} else if (status.equals("rinvia")) {
						System.out.println("Hai inviato una parola che non fa parte del vocabolario di Wordle\n"
								+ "Inviane un' altra, ricordati che sono ammesse solo parole di 10 caratteri\n");
					} else if (status.equals("gioca")) {
						System.out.println("Prima di inviare una parola, avvia una partita digitando: gioca");
					} else if (status.equals("terminata")) {
						System.out.println("Hai gia' giocato al gioco per la sessione corrente di Wordle\n"
								+ "Per rigiocare attendi una nuova parola segreta\n");
					}
				}
			}
		} catch (IOException exc) {
			exc.printStackTrace();
		}

	}

	// metodo con cui si richiede la visualizzazione a schermo della classifica di
	// wordle
	private void showMeRanking(String username, String password, DataInputStream dis, BufferedReader bs,
			DataOutputStream dos) {
		// preparo la stringa da mandare al server e la invio con una write
		String tosend = username + "/" + password + "/" + "classifica";
		// System.out.print(tosend.length());

		// System.out.println(tosend);
		try {
			// la mando
			dos.writeInt(tosend.length());

			dos.writeBytes(tosend);
			dos.flush();
			// bytes da leggere del successivo messaggio
			int bytes_to_read = dis.readInt();
			// System.out.println(bytes_to_read);
			// alloco un array di caratteri con tante posizioni quanti sono i byte da
			// leggere
			char[] array = new char[bytes_to_read];
			int bytes_read = 0;
			while ((bytes_read += bs.read(array, 0, array.length)) != bytes_to_read) {
				;
			}
			// converto l'array di caratteri in una stringa
			String str = String.valueOf(array).trim();
			// stampo la classifica
			System.out.println(str);
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}

	// metodo che stampa a schermo le statistiche di interesse dell'utente 
	private void sendMeStatistics(String username, String password, BufferedReader br, DataOutputStream dos) {

		// preparo la stringa da mandare al server e la invio con una write
		String tosend = username + "/" + password + "/" + "statistics";
		//System.out.print(tosend.length());

		//System.out.println(tosend);
		try {
			// la mando
			dos.writeInt(tosend.length());

			dos.writeBytes(tosend);
			dos.flush();
			// alloco abbastanza spazio a contenere il to_string delle statistiche 
			char[] array = new char[2048];
			br.read(array, 0, array.length);
			String str = String.valueOf(array).trim();
			System.out.println(str);
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}

	// metodo che viene invocato per l'avvio del client
	public void start() {

		// Stampe di benvenuto
		System.out.println("***********************************************");
		System.out.printf("Wordle: un gioco di parole 3.0\n");
		System.out.printf("Se hai gia' un account fai login per giocare\n"
				+ "Se non sei ancora registrato effettua la registrazione\n");
		System.out.println("***********************************************");
		System.out.printf("Per effettuare il login digita: login\n");
		System.out.printf("Per effettuare la registrazione digita: registrazione\n");

		// per poter giocare l'utente deve prima fare login
		// per fare login l'utente deve essere prima registato
		// all' avvio del client un utente non è nè loggato nè registrato
		boolean logged = false;
		boolean registered = false;

		// apro uno scanner sullo standard input
		Scanner sc = new Scanner(System.in);
		// comando inserito da tastiera
		String cmd = null;
		// username utente
		String username = null;
		// password utente
		String password = null;
		// parola guessed_word, inviata da client a server per indovinare parola segreta
		String guessed_word = null;
		// registry dove reperire stub oggetti remoti
		Registry registry = null;
		// stub servizio di registrazione
		RegistrationService stub = null;
		// stringhe coi nomi dei servizi da reperire sul registry
		String name = null;
		String name2 = null;
		// prendo il registro attivo su localhost alla porta 3333
		try {
			registry = LocateRegistry.getRegistry(port_registry);
			name = "REGISTRATION-SERVICE"; // nome servizio di registrazione
			name2 = "NOTIFICATION-SERVICE"; // nome servizio di notifica
			// prendo lo stub sul registro con nome name
			stub = (RegistrationService) registry.lookup(name);
		} catch (RemoteException re) {
			re.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}

		// finchè non sono loggato non posso giocare
		while (!logged) {
			// leggo il comando inserito da tastiera
			cmd = sc.next();
			// guardo che comando è stato inserito dall' utente
			switch (cmd) {
			case "login": // l'utente vuole loggarsi
				System.out.print("Inserisci il tuo username\n");
				username = sc.next();
				System.out.print("Inserisci la tua password\n");
				password = sc.next();
				// chiamo il metodo login con username e password presi da tastiera
				if (this.login(username, password, stub)) {
					try { // se la login è andata a buon fine
						logged = true; // loggato
						registered = true; // loggato --> registrato
						System.out.println("Login eseguito con successo\n");
						// reperisco lo stub del servizio di notifica dal registry
						NotificationService server = (NotificationService) registry.lookup(name2);
						/* si registra per la callback */
						System.out.println("Registering for callback");
						// istanzio l'oggetto che serve per notificare al client
						// di una variazione della classifica
						NotifyEventInterface callbackObj = new NotifyEventImpl();
						// esporto questo oggetto
						NotifyEventInterface stub2 = (NotifyEventInterface) UnicastRemoteObject
								.exportObject(callbackObj, port_callbackobj);
						// registro il client al servizio di notifica
						server.registerForCallback(stub2);
						// mi iscrivo e mi metto in ascolto sul gruppo di multicast
						// attraverso l'avvio di un thread
						Thread gestoreMulticast = new Thread(new MulticastManager());
						gestoreMulticast.start();

					} catch (Exception e) {
						System.out.println("Error in invoking object method");
					}

				} else
					System.out.println("Errore: Login fallito");
				break;
			case "registrazione":
				System.out.print("Inserisci il tuo username\n");
				username = sc.next();
				System.out.print("Inserisci la tua password\n");
				password = sc.next();
				// la password non può essere la stringa vuota
				while (password.equals("")) {
					System.out.println("password non valida, reinserire la password usando almeno un carattere");
					password = sc.next();
				}
				// invoco il metodo opportuno per registrarsi
				if (this.register(username, password, stub)) { // se la registrazione va a buon fine
					registered = true;
					System.out.println("Il tuo account è stato creato, per accedere digita login");
				} else
					System.out.println("Errore: Registrazione fallita");
				break;
			default:
				System.out.println("Comando inserito non valido");
				System.out.printf("Per effettuare il login digita: login\n");
				System.out.printf("Per effettuare la registrazione digita: registrazione\n");
			}

		}

		// Arrivato a questo punto l'utente è loggato
		System.out.printf("Benvenuto su Wordle %s!\n", username);
		System.out.printf("Per giocare digita: gioca\n");
		System.out.printf("Per effettuare il logout digita: logout\n");
		System.out.printf("Per visualizzare le tue statistiche  digita: statistiche\n");
		System.out.printf("Per visualizzare le classifica di Wordle digita: classifica\n");
		System.out.printf("Per richiedere la condivisione dei suggerimenti sul gruppo di Multicast digita : share\n");
		System.out.printf("Per visualizzare i suggerimenti ricevuti sul gruppo di Multicast digita : ShowMeSharing\n");
		System.out.printf("Per uscire digita: exit\n");

		// CREO UNA CONNESSIONE TCP CON IL SERVER
		String hostname = "localhost"; // il server sta su localhost
		Socket socket = null;

		try {
			// apro una socket col server attivo in localhost su porta
			// (4444)
			// Creates a stream socket and connects it to the specified portnumber on the
			// named host
			socket = new Socket(hostname, port_socket_tcp);

			System.out.println("connected to server");
			// With this option setto a positive timeout value, a read() call on the
			// InputStream associated withthis Socket will block for only this amount of
			// time. If the timeoutexpires, a java.net.SocketTimeoutException is raised,
			// though theSocket is still valid.
			socket.setSoTimeout(timeout_socket_tcp);// imposto un timeout di un minuto sulla socket

			// CREO UN INPUTSREAM : Returns an input stream for this socket.
			// an input stream for reading bytes from this socket.
			InputStream in = socket.getInputStream();

			// CREO UN OUTPUTSTREAM : Returns an output stream for this socket.
			// an output stream for writing bytes to this socket.
			OutputStream out = socket.getOutputStream();

			// CREO UN BUFFEREDREADER
			// lo attengo chiamando il relativo costruttore passandogli un InputSteamReader
			// che ho "avvolto" sull'InputStream
			// Creates a buffering character-input stream that uses a default-sizedinput
			// buffer.
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			// CREO UN DATAOUTPUTSTREAM
			// Creates a new data output stream to write data to the specifiedunderlying
			// output stream.
			DataOutputStream dos = new DataOutputStream(out);

			// creo un DataInputStream
			// Creates a DataInputStream that uses the specified underlying InputStream.
			DataInputStream dis = new DataInputStream(in);

			boolean stop = false;
			while (!stop) {
				// leggo la richiesta dell' utente da tastiera
				cmd = sc.next();
				// gestisco la richiesta che mi arriva
				switch (cmd) {
				case ("gioca"): // voglio iniziare una partita di wordle
					if (this.playWordle(username, password, dos, reader)) {
						// partita avviata
						System.out.println("Inizio partita");
						System.out.println("Per inviare la parola myword digita: send myword");
					} else // non è stato possibile avviare la partita
						System.out.println("Non è stato possibile iniziare la partita");
					break;
				case ("logout"):
					if (this.logout(username, password, reader, dos)) {
						System.out.println("Logout eseguito con successo");
					} else
						System.out.println("Logout fallito");
					break;
				case ("exit"):
					// Closes this scanner.
					sc.close();
					// metto la variabile stop a true
					stop = true;
					// chiudo il socket quindi la connessione TCP
					socket.close();
					// Closes this datagram socket.
					// Any thread currently blocked in receive upon this socketwill throw a
					// SocketException.
					multicastWelcome.close();
					break;
				case ("send"): // l'utente vuole inviare una parola
					// leggo la parola immessa da tastiera
					guessed_word = sc.next();

					this.sendWord(username, password, guessed_word, reader, dos);
					break;
				case ("statistiche"):
					this.sendMeStatistics(username, password, reader, dos);
					break;
				case ("classifica"):
					this.showMeRanking(username, password, dis, reader, dos);
					break;
				case ("share"):
					this.share(username, password, dos, reader);
					break;
				case ("showMeSharing"):
					this.showMesharing();
					break;
				default:

					System.out.println("Comando inserito non valido");
					System.out.printf("Per giocare digita: gioca\n");
					System.out.printf("Per effettuare il logout digita: logout\n");
					System.out.printf("Per visualizzare le tue statistiche  digita: statistiche\n");
					System.out.printf("Per visualizzare le classifica di Wordle digita: classifica\n");
					System.out.printf(
							"Per richiedere la condivisione dei suggerimenti sul gruppo di Multicast digita : share\n");
					System.out.printf(
							"Per visualizzare i suggerimenti ricevuti sul gruppo di Multicast digita : ShowMeSharing\n");
					System.out.printf("Per uscire digita: exit\n");
				}
			}
			// Terminates the currently running Java Virtual Machine. Theargument serves as
			// a status code; by convention, a nonzero statuscode indicates abnormal
			// termination.
			System.exit(0);

		} catch (IOException ex) {
			System.out.println("could not connect to server");
		} finally {
			if (socket != null) {
				try {
					socket.close();
					sc.close();
					multicastWelcome.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

		}

	}
}
