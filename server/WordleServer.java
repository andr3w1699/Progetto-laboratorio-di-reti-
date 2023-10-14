//package progetto_laboratorio_di_reti;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.io.IOException;
import java.net.*;
import java.net.UnknownHostException;
import java.nio.channels.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Classe che implementa il server
 * 
 * @author Andrea Lepori
 * @version 1.0
 *
 */
public class WordleServer {

	// dichiaro un oggetto della classe RegistrationServiceImpl che fungerà da
	// database delle registrazioni
	private RegistrationServiceImpl RegistrationService;

	// dichiaro un oggetto della classe ExecutorService che sarà il mio threadpool
	// che eseguirà i task in arrivo al server
	private ExecutorService service;

	// game rappresenta il gioco di Wordle
	private Game game;

	// il gioco ha una classifica
	private Classifica classifica;

	// servizio di notifica
	private NotificationServiceImpl server;

	// indirizzo gruppo multicast
	private String MulticastAddress;

	// InetAddress del gruppo di multicast
	private InetAddress multicastGroup;

	// porta associata all'indirizzo multicast
	private int port;

	// porta associata al registry
	private int registry_port;

	// porta associata al socket tcp
	private int socket_tcp_port;

	// tempo da aspettare per serializzare in ms
	private int time_to_awayt;

	// porta associata all' oggetto remoto che gestisce le registrazioni
	private int registr_port;
	
	// tempo di vita di una sw
	private int time_to_refresh;

	// porta associata all' oggetto remoto che gestisce le notifiche
	private int notific_port;

	// metodo costruttore della classe WordleServer
	// settaggio dei parametri di configurazione presi da file di configurazione del
	// server

	public WordleServer(String MulticastAddress, int multicastPort, int registry_port, int socket_tcp_port,
			int time_to_awayt, int registr_port, int notific_port, int time_to_refresh) {
		this.MulticastAddress = MulticastAddress;
		// sets the port number for multicast group
		this.port = multicastPort;
		this.registry_port = registry_port;
		this.socket_tcp_port = socket_tcp_port;
		this.time_to_awayt = time_to_awayt;
		this.registr_port = registr_port;
		this.notific_port = notific_port;
		this.time_to_refresh=time_to_refresh;
	}

	// RUNNABLE //
	// Rappresentano tutti i task che il mio server dovrà eseguire
	// in particolare verranno passati al threadpool che li eseguirà

	// task che si occupa della serializzazione
	public class serializzatore_stato_server implements Runnable {
		// database registrazioni
		private RegistrationService db;
		// tempo in ms che mi dice ogni quanto serializzare
		private int millis;

		// metodo costruttore
		public serializzatore_stato_server(RegistrationService db, int millis) {
			// fa la cosa ovvia
			this.db = db;
			this.millis = millis;
		}

		// @overriride del metodo run
		public void run() {
			// entro in un ciclo in cui dormo, mi sveglio, serializzo e rivado a dormire
			while (true) {
				try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				// path che indica la cartella di lavoro corrente (CURRENT WORKING DIRECTORY)
				String curDir = System.getProperty("user.dir");

				File stato_registrazioni = new File(curDir, "LogRegistrazioni.txt");

				System.out.println("Serializzazione in corso");
				try ( // uso un try-with-resources

						// creo un FileOutputStream verso il file col db delle registrazioni
						// new file is created when we instantiate the FileOutputStream object. If a
						// file with a given name already exists, it will be overwritten.
						FileOutputStream fos = new FileOutputStream(stato_registrazioni);
						// apro un OutputStreamWriter
						OutputStreamWriter ow = new OutputStreamWriter(fos);) {
					// serializzo
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					String jsonString = gson.toJson(db.getRegistrazioni());
					// scrivo sull' OutputStream
					ow.write(jsonString);
					ow.flush();

					System.out.println("Serializzazione terminata con successo");

				}

				catch (RemoteException e) {

					e.printStackTrace();
				}

				catch (FileNotFoundException e) {

					e.printStackTrace();
				} catch (IOException ioe) {

					ioe.printStackTrace();
				}

			}
		}

	}

	// task che realizza la  condivisione dei suggerimenti sul gruppo di multicast
	public class shareSuggestions implements Runnable {
		// username dell' utente
		private String username;
		// database utenti registrati
		private RegistrationServiceImpl RegistrationService;
		// selection key del channel che era readable
		SelectionKey key;
		// gioco
		Game game;
		// indirizzo del gruppo di multicast
		InetAddress multicastGroup;
		// porta gruppo multicast
		int port;

		// metodo costruttore
		public shareSuggestions(String username, RegistrationServiceImpl registrationService, SelectionKey key,
				Game game, InetAddress multicastGroup, int port) {
			// fa la cosa ovvia
			this.username = username;
			this.RegistrationService = registrationService;
			this.key = key;
			this.game = game;
			this.multicastGroup = multicastGroup;
			this.port = port;
		}

		// @override del metodo run
		public void run() throws NullPointerException {
			// prendo il Socketchannel associato alla SelectionKey key
			SocketChannel c_channel = (SocketChannel) key.channel();
			// codice che contiene l'esito dell' operazione
			String exit_code = "ok";
			// creo il msg da inviare
			String message = username + "/" + "share" + "/" + exit_code + "\n";
			// converto la stringa in un array di bytes
			// avvolgo l'array di bytes in un ByteBuffer
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			// byte da inviare sulla connessione
			int bytes_to_send = message.getBytes().length;
			// byte inviati sulla connessione
			int bytes_sent = 0;
			// scrivo sul channel la sequenza di byte
			try {
				while ((bytes_sent += c_channel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("Server: " + message + " inviato al client " + c_channel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			// procedo con l'invio dei suggerimenti sul gruppo di multicast
			// utilizzo un try-with-resources per aprire un DatagramSocket
			try (DatagramSocket sock = new DatagramSocket()) {
				// vado a prendere la partita associata all' utente con username
				Partita p = this.game.getPartita(RegistrationService.getUser(username));
				if (p == null)
					throw new NullPointerException("Partita non trovata, impossibile inviare le condivisioni");
				// preparo il DatagramPacket da inviare
				DatagramPacket dat = new DatagramPacket((username + "\n"+ p.getTentativi()).trim().getBytes("ASCII"),
						(username + "\n"+ p.getTentativi()).trim().length(), this.multicastGroup, this.port);
				// invio il datagramma sul socket
				sock.send(dat);
				System.out.printf("Sent on multicast socket:\n%s\n", new String(dat.getData(), dat.getOffset(), dat.getLength()));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// task che rappresenta il tentativo di un utente di indovinare la secret word
	public class guess implements Runnable {
		// username dell' utente
		private String username;
		// selection key del channel che era Readable
		SelectionKey key;
		// gioco
		Game game;
		// guessed word
		String gw;
		// utente
		Utente u;

		//
		public guess(String username, SelectionKey key, Game game, String gw) {
			// fa la cosa ovvia
			this.username = username;
			this.key = key;
			this.game = game;
			this.gw = gw;
			this.u = RegistrationService.getUser(username);
		}

		// @override del metodo run
		public void run() throws NoSuchElementException {
			if (u == null)
				throw new NoSuchElementException("Utente " + username + " non trovato");
			String codice; // contiene l'esito della richiesta
			String suggestions = "nada"; // valore di default quando non devo dare suggerimenti
			// prendo il canale associato alla key
			SocketChannel c_channel = (SocketChannel) key.channel();
			Partita p = game.getPartita(u);
			// controllo che u stia giocando alla sessione corrente
			Boolean in_game = (p != null);
			if (in_game) { // se sto giocando
				// controllo che la partita non sia finita 
				Boolean partita_finita = p.isEnd();
				if (!partita_finita) {
					// invece di controllare la lunghezza della parola controllo direttamente se è
					// nel dizionario, se non è lunga 10 sicuramente non è nel dizionario
					// controllo se la gw inviata è presente nel dizionario
					Boolean in_dictionary = game.inVocaboulary(gw);
					if (in_dictionary) { // se la gw è nel dizionario
						suggestions = game.suggestions(gw); // prendo la stringa con i suggerimenti
						boolean test = game.test(u, gw); // testo se ho indovinato la parola
						if (test)
							codice = "ok"; // se ho vinto restituisco ok
						else if (game.getPartita(u).isEnd())
							codice = "fine"; // se non ho vinto e ho esaurito i tentativi
						else
							codice = "ritenta"; // non ho vinto ma ho ancora tentativi
					} else
						codice = "rinvia"; // rinviare la parola xk non è nel dizionario
				} else
					codice = "terminata";
			} else
				codice = "gioca"; // gioca per la sessione corrente
			// preparo il messaggio da inviare
			String message;
			if (codice.equals("fine") || codice.equals("ok"))
				message = username + "/" + "guess" + "/" + codice + "/" + suggestions + "/" + game.getTranslation()
						+ "\n";
			else
				message = username + "/" + "guess" + "/" + codice + "/" + suggestions + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			int bytes_to_send = message.getBytes().length;
			int bytes_sent = 0;
			// scrivo sul channel la sequenza di byte
			try {
				while ((bytes_sent += c_channel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("Server: " + message + " inviato al client " + c_channel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}

	// task che serve per avviare una partita nella sessione corrente del gioco 
	public class gioca implements Runnable {
		// username dell' utente
		private String username;
		// selection key del channel che era Readable
		SelectionKey key;
		// gioco
		Game game;
		// Utente
		Utente u;

		// metodo costruttore
		public gioca(String username, SelectionKey key, Game game) {
			// fa la cosa ovvia
			this.username = username;
			this.key = key;
			this.game = game;
			this.u = RegistrationService.getUser(username);
		}

		// @override del metodo run
		public void run() {
			boolean exit_status = true;
			// controllo se u ha già giocato o sta già giocando alla sessione corrente del
			// gioco
			if (game.alreadyPlayed(u))
				exit_status = false;
			if (exit_status) {
				// creo una partita per l'utente e comunico al client che può iniziare a giocare
				game.addPlayers(u);
				System.out.println(username + "ha iniziato una partita");
			}
			String exit_code;
			if (exit_status)
				exit_code = "ok"; // partita iniziata con successo
			else
				exit_code = "no"; // non è stato possibile iniziare la partita

			// prendo il canale associato alla key
			SocketChannel c_channel = (SocketChannel) key.channel();
			// preparo il messaggio
			String message = username + "/" + "gioca" + "/" + exit_code + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			// numero di byte da inviare
			int bytes_to_send = message.getBytes().length;
			// numero di byte inviati
			int bytes_sent = 0;
			// scrivo sul channel la sequenza di byte
			try {
				while ((bytes_sent += c_channel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("Server: " + message + " inviato al client " + c_channel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}

	}

	// task che gestisce la richiesta di logout di un Utente
	public class logout implements Runnable {
		// username dell' utente
		private String username;
		// password dell' utente
		private String password;
		// database utenti registrati
		private RegistrationServiceImpl RegistrationService;
		// selection key del channel che era Readable
		SelectionKey key;
		// gioco di Wordle
		Game game;
		// Utente
		Utente u;

		// metodo costruttore
		public logout(String username, String password, RegistrationServiceImpl RegistrationService, SelectionKey key,
				Game game) {
			// fa la cosa ovvia
			this.username = username;
			this.password = password;
			this.RegistrationService = RegistrationService;
			this.key = key;
			this.game = game;
			this.u = RegistrationService.getUser(username);
		}

		// @override del metodo run che definisce il task
		public void run() {
			// flag dove vado a registrare il buon esito o meno dell' operazione di unlog
			boolean exit_status;
			// unloggo l'utente chiamando l'opportuno metodo fornito da RegistrationService
			exit_status = RegistrationService.setUnlogged(username, password);
			// prendo il canale associato alla key
			SocketChannel c_channel = (SocketChannel) key.channel();
			String exit_code;
			if (exit_status == true)
				exit_code = "ok";
			else
				exit_code = "no";
			// preparo il msg
			String message = username + "/" + "logout" + "/" + exit_code + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			int bytes_to_send = message.getBytes().length;
			int bytes_sent = 0;
			// scrivo sul channel la sequenza di byte
			try {
				while ((bytes_sent += c_channel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("Server: " + message + " inviato al client " + c_channel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			// se l'utente stava giocando una partita non terminata il logout la termina
			if (exit_status)
				game.endMatch(u);

		}
	}

	// task che gestisce l' invio delle statistiche
	public class sendStatistics implements Runnable {
		// username of the user to whom to send the statistics
		String username;
		// user's password
		String password;
		// db registrazione
		RegistrationService db;
		// selection key which was readable
		SelectionKey key;

		public sendStatistics(String username, String password, RegistrationService db, SelectionKey key) {
			// fa la cosa ovvia
			this.username = username;
			this.password = password;
			this.db = db;
			this.key = key;
		}

		// @override
		public void run() throws NoSuchElementException {
			try {
				// prendo l'utente con quell'username dal database
				Utente u = db.getUser(username);
				// prendo le statische di qeull' utente
				Statistiche stat = u.getStatistiche();
				// chiamo il toString delle Statistiche
				String msg = stat.toString();

				SocketChannel c_channel = (SocketChannel) key.channel();
				ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());

				int bytes_to_send = msg.getBytes().length;
				int bytes_sent = 0;
				while ((bytes_sent += c_channel.write(buffer)) != bytes_to_send) {
					;
				}

				System.out.println("Server: " + msg + " inviato al client " + c_channel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}
	}

	// task che gestisce l'invio della classifica
	public class sendRanking implements Runnable {

		RegistrationService db;
		SelectionKey key;

		public sendRanking(RegistrationService db, SelectionKey key) {

			this.db = db;
			this.key = key;
		}

		public void run() {
			try {
				// prendo la classifica e faccio il toString
				String msg = db.getClassifica().toString();
				// prima invio un intero che codifica la lunghezza della classifica 
				int msgLength = msg.length();
				SocketChannel c_channel = (SocketChannel) key.channel();
				byte[] bytes = ByteBuffer.allocate(4).putInt(msgLength).array();
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				int bytes_to_send = bytes.length;
				int bytes_sent = 0;
				while ((bytes_sent += c_channel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("Server: " + msgLength + " inviato al client " + c_channel.getRemoteAddress());
				System.out.flush();
				// poi invio la classifica 
				buffer = ByteBuffer.wrap(msg.getBytes());
				bytes_to_send = msg.getBytes().length;
				bytes_sent = 0;
				while ((bytes_sent += c_channel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("Server: " + msg + " inviato al client " + c_channel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}
	}

	// metodo che viene eseguito per avviare il server
	public void start() {
		try {

			// controllo il numero di core disponibili sulla macchina
			// e creo un FixedThreadpool con tanti thread quanti sono i core
			int procs = Runtime.getRuntime().availableProcessors();
			System.out.println(" Cores available on this machine : " + procs);
			service = Executors.newFixedThreadPool(procs);
			System.out.println("ThreadPool creato");

			// creo la classifica
			classifica = new Classifica();
			System.out.println("Classifica creata");

			/* creazione di un'istanza dell'oggetto RegistrationServiceImpl */
			RegistrationService = new RegistrationServiceImpl(classifica);
			System.out.println("Servizio di registrazione creato");

			/* creazione di un'istanza dell'oggetto NotificationServiceImpl */
			server = new NotificationServiceImpl();
			System.out.println("Servizio di notifica creato");

			// creo il gioco
			game = new Game(classifica, server, time_to_refresh);
			System.out.println(
					"Game creato con secretWord = " + game.getSecretWord() + " in italiano : " + game.getTranslation());

			/* Esportazione degli oggetti remoti */
			// creazione stub
			RegistrationService stub = (RegistrationService) UnicastRemoteObject.exportObject(RegistrationService,
					registr_port);
			NotificationService stub2 = (NotificationService) UnicastRemoteObject.exportObject(server, notific_port);

			/*
			 * Creazione di un registry sulla porta 3333
			 * 
			 */
			LocateRegistry.createRegistry(registry_port);
			// vado a prendere il registry che ho appena creato
			Registry r = LocateRegistry.getRegistry(registry_port);

			/* Pubblicazione degli stub nel registry */
			r.rebind("REGISTRATION-SERVICE", stub);
			r.rebind("NOTIFICATION-SERVICE", stub2);

			// sets the IP adress for Multicast Group
			this.multicastGroup = InetAddress.getByName(MulticastAddress);
			if (!this.multicastGroup.isMulticastAddress())
				throw new IllegalArgumentException("L' indirizzo immesso non è valido come indirizzo di Multicast");

			System.out.println("Server Ready\n");

			// Creo ed avvio il thread per la serializzazione
			Thread serializer = new Thread(new serializzatore_stato_server(RegistrationService, time_to_awayt));
			serializer.start();
		}
		/* If any communication failures occur... */
		catch (RemoteException e) {
			System.out.println("Communication error " + e.toString());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// PREDISPONGO IL MIO SERVER PER RICEVERE E GESTIRE LE CONNESSIONI

		// dichiaro un ServerSocketChannel che sarà il mio listening socket per le
		// richieste di connessione
		ServerSocketChannel serverChannel;
		// dichiaro un selettore che servirà per il multiplexing dei canali
		Selector selector;

		try {
			// apro un channel per il listening socket
			serverChannel = ServerSocketChannel.open();
			// prendo il ServerSocket associato al ServerSocketChannel aperto sopra
			ServerSocket ss = serverChannel.socket();
			// metto in ascolto il listening socket in localhost sulla porta indicata
			// Creates a socket address where the IP address is the wildcard address and the
			// port number a specified value. The wildcard is a special local IP address
			InetSocketAddress address = new InetSocketAddress(socket_tcp_port);
			ss.bind(address);
			// confinguro il channel a non bloccante
			serverChannel.configureBlocking(false);
			// apro il selettore e registro il serversocketchannel su questo selettore per
			// le operazioni di ACCEPT
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException ex) {
			ex.printStackTrace();
			return;

		}

		// Il server entrerà in questo ciclo infinito dove il selettore monitorerà i
		// channel a lui registrati

		while (true) {
			try {
				// Selects a set of keys whose corresponding channels are ready for
				// I/O operations.
				selector.select();
			} catch (IOException ex) {
				ex.printStackTrace();
				break;
			}

			// prendo il set dei channel "pronti" o meglio le chiavi/i token corrispondenti
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			// prendo un iteratore su questo set
			Iterator<SelectionKey> iterator = readyKeys.iterator();

			// ciclo finchè ci sono elem nell' iteratore
			while (iterator.hasNext()) {
				// prendo il next
				SelectionKey key = iterator.next();
				// rimuove la chiave dal Selected Set, ma non dal Registered Set
				iterator.remove();
				try {
					// Il canale è pronto per accettazione di connessioni
					if (key.isAcceptable()) {
						// prendo il server socket channel associato alla key in questione
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						// faccio un' accept su quel server socket che mi restituirà il socketchannel
						// per la comunicazione col client
						SocketChannel client = server.accept();
						System.out.println("Accepted connection from" + client);
						// configuro il SocketChannel appena creato come non bloccante
						client.configureBlocking(false);
						// crea il ByteBuffer[] che servirà per implementare il mio protocollo di
						// comunicazione
						// alloca un byte buffer con tanto spazio per contenere un intero
						ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
						// alloco un byte buffer con tanto spazio per contenere un msg del mio
						// protocollo
						ByteBuffer message = ByteBuffer.allocate(2048);
						// metto tutto in un array di ByteBuffer
						ByteBuffer[] bfs = { length, message };
						// registro il SocketChannel appena creato sul selettore per le operazioni di
						// lettura e aggiunge l'array di bytebuffer [lenght, message] come attachment
						client.register(selector, SelectionKey.OP_READ, bfs);

					}

					// Il canale è pronto in lettura
					if (key.isReadable()) {
						// recupero il SocketChannel associato alla chiave
						SocketChannel client = (SocketChannel) key.channel();
						// recupera l'array di bytebuffer (attachment)
						ByteBuffer[] bfs = (ByteBuffer[]) key.attachment();
						// nel protocollo stabilito il client invia messaggi al server del tipo
						// username/password/cmd[/valore] dove valore è opzionale , sarà presente solo
						// per determinati comandi (cmd)
						String username = null;
						String password = null;
						String cmd = null;
						String gw = null;

						// leggo dal socket channel nel buffer bfs in maniera non bloccante
						// Reads a sequence of bytes from this channel into the given buffers.
						// Returns:The number of bytes read, possibly zero,or -1 if the channel has
						// reached end-of-stream
						client.read(bfs);
						// se ho letto tutto l'intero nel primo ByteBuffer
						if (!bfs[0].hasRemaining()) {
							// mi preparo a leggere dal buffer facendo una flip
							bfs[0].flip();
							// prendo l'intero che mi dice la length in byte del msg che segue
							int l = bfs[0].getInt();
							// System.out.println(l);
							if (bfs[1].position() == l) { // ho letto tutto quello che c'era da leggere
								bfs[1].flip();
								// trasformo in stringa il msg ricevuto dal client (trim per eliminare spazio lo
								// bianco in fondo alla linea )
								// il metodo array() applicato ad un ByteBuffer lo trasforma in un byte[]
								// Constructs a new String by decoding the specified array of bytes using the
								// platform's default charset. The length of the new String is a function of the
								// charset, and hence may not be equal to thelength of the byte array.
								String message = new String(bfs[1].array()).trim();
								// System.out.printf(message + "\n");
								// rinizializzo il ByteBufferArray per successive comunicazioni
								ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
								ByteBuffer messagenew = ByteBuffer.allocate(2048);
								ByteBuffer[] bfsnew = { length, messagenew };
								key.attach(bfsnew);

								// tokenizzo la stringa contenente il msg, uso / come separatore di token
								StringTokenizer tokenizedLine = new StringTokenizer(message, "/");
								// mi aspetto di trovare un token contenente l'username dell' utente come da mio
								// protocollo
								if (tokenizedLine.hasMoreTokens()) {
									username = tokenizedLine.nextToken();
									// System.out.println(username);
								} else {
									System.err.println("Errore di comunicazione: username non trovato");
									throw new NoSuchElementException();
								}

								// mi aspetto di trovare un token contenente la password dell' utente come da
								// protocollo da me stabilito
								if (tokenizedLine.hasMoreTokens()) {
									password = tokenizedLine.nextToken();
									// System.out.println(password);
								} else {
									System.err.println("Errore di comunicazione: password non trovata");
									throw new NoSuchElementException();
								}
								// mi aspetto di trovare un token contenente il comando inviato dal client come
								// da mio protocollo
								if (tokenizedLine.hasMoreTokens()) {
									cmd = tokenizedLine.nextToken();
									// System.out.println(cmd);
								} else {
									System.err.println("Errore di comunicazione: comando non trovato");
									throw new NoSuchElementException();
								}

								// se un utente non è registrato non può compiere azioni

								if (!RegistrationService.isRegistered(username)) {
									System.err.printf("Utente %s non registrato\n", username);
									break;
								}

								// se un utente non è loggato non può compiere azioni

								if (!RegistrationService.getUser(username).isLogged()) {
									System.err.printf("Utente %s non loggato\n", username);
									break;
								}

								// con uno switch case vado a vedere che comando mi sta mandando il client
								// in base al tipo di comando inviato creerò un task specifico da passare al
								// threadpool che lo farà eseguire ad un suo thread 
								switch (cmd) {
								case ("logout"):
									service.execute(new logout(username, password, RegistrationService, key, game));
									break;
								case ("gioca"):
									service.execute(new gioca(username, key, game));
									break;
								case ("guess"):
									if (tokenizedLine.hasMoreTokens()) {
										gw = tokenizedLine.nextToken();
										// System.out.println(gw);
										service.execute(new guess(username, key, game, gw));
									} else {
										System.err.println("Errore di comunicazione: guessed word non trovata");
										throw new NoSuchElementException();

									}

									break;
								case ("statistics"):
									service.execute(new sendStatistics(username, password, RegistrationService, key));
									break;
								case ("classifica"):
									service.execute(new sendRanking(RegistrationService, key));
									break;
								case ("share"):
									service.execute(new shareSuggestions(username, RegistrationService, key, game,
											multicastGroup, port));
									break;
								}
							}
						}
					}
				}

				catch (IOException ex) {
					key.cancel();
					try {
						key.channel().close();
					} catch (IOException cex) {
					}
				}
			}
		}

	}

}
