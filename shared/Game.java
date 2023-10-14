//package progetto_laboratorio_di_reti;

import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.NoSuchElementException;

/**
 * Classe che rappresenta il gioco di Wordle
 * <p>
 * Il gioco di Wordle ha una secretWord (String, parola inglese) che viene
 * cambiata ad ogni nuova sessione del gioco, una struttura dati,
 * Map<Utente,Partita> players, memorizza gli utenti che stanno partecipando
 * alla sessione attuale del gioco e le rispettive partite. La stringa
 * traduzione mantiene la traduzione italiana della secretWord corrente, il
 * gioco dispone di una struttura words (lista di stringhe)che mantiene le
 * parole del dizionario di Wordle tra cui viene scelta la secretWord, Wordle ha
 * una classifica, il gioco dispone inoltre di un servizio di notifica che
 * avverte tutti gli utenti loggati quando ci sono modifiche nel podio della
 * classifica.
 * 
 * @author Andrea Lepori
 * 
 * @version 1.0
 */

public class Game {

	// Il gioco prevede una parola segreta (inglese) che gli utenti devono
	// indovinare
	private String secretWord;

	// struttura che memorizza gli utenti che stanno giocando e le rispettive
	// partite in corso
	private Map<Utente, Partita> players;

	// Il gioco prevede un vocabolario di parole lecite
	private List<String> words;

	// La classifica del gioco
	private Classifica c;

	// traduzione italiana della secret word
	private String traduzione;

	// servizio di notifica remoto
	private NotificationServiceImpl ns;

	// tempo da attendere prima di fare il refresh del gioco (in millisecondi)
	private int time_to_awayt ;

	// classe interna che rappresenta il task che implementa il refresh del gioco,
	// quindi la chiusura di una sessione e l'apertura di una nuova, implementa
	// l'interfacia Runnable
	private class refreshGame implements Runnable {
		// tempo di validità della parola segreta
		int time_to_await;

		// metodo costruttore della classe interna
		public refreshGame(int millis) {
			// il tempo di vita di una secretword è quello in millisecondi passato al
			// costruttore
			time_to_await = millis;

		}

		// @override del metodo run
		public synchronized void run() {
			// entro in un ciclo in cui dormo per il tempo di vita di una sessione,
			// mi sveglio, chiudo la vecchia sessione (resettando la struttura che mantiene
			// gli utenti che stanno giocando all'attuale sessione del gioco con le
			// rispettive partite) e ne avvio una nuova con una nuova SecretWord.
			while (true) {
				try {
					Thread.sleep(time_to_await); // dormo per tanto tempo quanto deve rimanere in vita la parola
													// corrente
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Refresh del gioco in corso");
				// chiamo la funzione che termina la vecchia sessione del gioco
				// le partite che non sono finite sono considerate perse
				endGame();
				// resetto la struttura che mantiene gli utenti che stanno giocando e le
				// relative partite in corso perchè inizia una nuova sessione
				resetPlayer();
				// genero una nuova parola segreta
				String new_sw = generateRandomWord();
				// aggiorno la parola segreta
				setSecretWord(new_sw);
				// traduco la nuova secret Word e aggiorno la variabile che contiene la
				// traduzione
				String traduzione = Translate(getSecretWord());
				setTranslation(traduzione);
				System.out.println("New game started : \n" + "new secret word-->" + secretWord
						+ " italian translation -->" + getTranslation());
			}
		}
	}

	/**
	 * metodo costruttore di Game
	 * 
	 * @param c  classifica
	 * 
	 * @param ns servizio di notifica
	 */
	public Game(Classifica c, NotificationServiceImpl ns, int time_to_awayt) throws NullPointerException {

		if (c == null || ns == null)
			throw new NullPointerException();

		// la classifica e il servizio di notifica sono quelli passati al costruttore
		this.ns = ns;
		this.c = c;
		// setto il tempo di vita di una parola segreta
		this.time_to_awayt = time_to_awayt;

		// creiamo una LinkedList che contenga le parole del dizionario
		words = new LinkedList<>();

		// creiamo una ConcurrentHashMap che avrà come elementi tuple <Utente,Partita>
		players = new ConcurrentHashMap<>();

		// creo un'istanza di un oggetto File, passandogli il path del file
		// ricordarsi che in Eclipse per default la directory corrente è quella del
		// progetto (non della src)
		// ATTENZIONE VADO A CERCARE IL FILE NELLA WORKING DIRECTORY QUINDI LA DIRECTORY
		// IN CUI LANCIO IL PROGRAMMA da TERMINALE, IL PROGRAMMA DEVE ESSERE LANCIATO
		// NELLA CARTELLA INIZIALE e I FILE DEVONO ESSERE MESSI LI
		// creo un FileReader e lo "avvolgo" in un BufferedReader
		// uso il meccanismo dell'AUTOCLOSURE nel costrutto try-catch
		// leggo una ad una le parole dal file (una per linea) e le metto nel dizionario

		try (BufferedReader br = new BufferedReader(
				new FileReader(new File(System.getProperty("user.dir"), "words.txt")))) {
			String line;
			while ((line = br.readLine()) != null) { // prendo una linea alla volta sino a che non raggiungo
														// l'end-of-the-stream
				// aggiungo la parola presente in quella linea alla mia struttura
				words.add(line.trim()); // la funzione trim toglie gli spazi bianchi dal fondo della linea
			}
		} catch (FileNotFoundException exc) {
			exc.printStackTrace();
		} catch (IOException exc) {
			exc.printStackTrace();
		}

		// imposto come parola segreta una parola casuale del dizionario
		this.secretWord = this.generateRandomWord();
		// la traduco
		this.traduzione = this.Translate(secretWord);
		// creo e starto il thread che si occuperà di fare il refresh del gioco
		Thread t = new Thread(new refreshGame(this.time_to_awayt));
		t.start();
	}

	// METODI GETTER
	// questi metodi sono synchronized poichè la struttura game è una struttura dati
	// condivisa tra più threads

	// restituisce la parola segreta
	public synchronized String getSecretWord() {
		return this.secretWord;
	}

	// restituisce la traduzione della parola segreta
	public synchronized String getTranslation() {
		return this.traduzione;
	}

	/**
	 * metodo che restituisce la partita associata all' utente giocatore se esiste
	 * per la sessione corrente del gioco, null altrimenti
	 * 
	 * @param giocatore , utente del quale vogliamo reperire, se esiste, la partita
	 *                  associata
	 * 
	 * @return la Partita associata all' utente giocatore se esiste, null altrimenti
	 */
	public synchronized Partita getPartita(Utente giocatore) throws NullPointerException {
		if (giocatore == null)
			throw new NullPointerException();
		// Returns the value to which the specified key is mapped,or null if this map
		// contains no mapping for the key
		return players.get(giocatore);
	}

	// restituisce la classifica di Wordle
	public synchronized Classifica getClassifica() {
		return this.c;
	}

	// metodo che mi dice se l'Utente u passato in input ha già giocato o sta
	// giocando al gioco nella sessione corrente
	public synchronized boolean alreadyPlayed(Utente u) throws NullPointerException {
		if (u == null)
			throw new NullPointerException();
		// Returns true if this map contains a mapping for the specifiedkey.
		return this.players.containsKey(u);
	}

	// metodo che mi dice se una parola è presente nel vocabolario del gioco
	// questo metodo non è dichiarato synchronized poichè dopo la costruzione
	// accedo al dizionario solo in lettura
	public boolean inVocaboulary(String word) throws NullPointerException {
		if (word == null)
			throw new NullPointerException();
		return this.words.contains(word.trim());
	}

	// METODI SETTER
	// questi metodi sono synchronized poichè la struttura game è una struttura dati
	// condivisa tra più threads

	/**
	 * metodo che aggiunge l'Utente giocatore alla hashmap degli utenti che stanno
	 * giocando per la sessione corrente del gioco
	 * 
	 * @excpetion NullPointerException if giocatore == null
	 * @exception IllegalArgumentException if this.alreadyPlayed(giocatore)
	 */
	public synchronized void addPlayers(Utente giocatore) throws NullPointerException, IllegalArgumentException {
		if (giocatore == null)
			throw new NullPointerException();
		if (this.alreadyPlayed(giocatore))
			throw new IllegalArgumentException(giocatore.getUsername() + "Sta già giocando alla partita in corso");
		{
		}
		// inserisco nella mappa il giocatore e inizializzo una nuovo oggetto partita
		// a lui associato
		players.put(giocatore, new Partita());
	}

	// metodo che resetta la struttura che memorizza i giocatori di Wordle e le
	// rispettive partite per la sessione corrente
	public synchronized void resetPlayer() {
		// creo una nuova ConcurrentHashMap invocando il costruttore e memorizzo il
		// riferimento nella variabile players
		this.players = new ConcurrentHashMap<>();
	}

	// sets this.secretWord to newSecretWord
	public synchronized void setSecretWord(String newSecretWord) {
		this.secretWord = newSecretWord;
	}

	// sets this.traduzione to word
	public synchronized void setTranslation(String word) {
		this.traduzione = word;
	}

	// METODI di SUPPORTO
	// tutti metodi synchronized

	/**
	 * metodo che data una guessed word restituisce la stringa dei suggerimenti
	 * rispetto alla secret word attuale del gioco
	 * 
	 * @param guessedWord una stringa che rappresenta la guessedWord, presuppone che
	 *                    sia stata controllata la lunghezza della gw e se la gw
	 *                    appartiene al dizionario del gioco
	 * 
	 * @return la stringa dei suggerimenti ottenuta sostituendo '+' alle lettere
	 *         della gw presenti nella sw nella giusta posizione, '?' alle lettere
	 *         della gw presenti nella sw ma non nella posizione giusta e 'X' alle
	 *         lettere della gw non presenti nella sw
	 */
	public synchronized String suggestions(String guessedWord) {

		// inizializzo la stringa dei suggerimenti con la stringa vuota
		String stringa_suggerimenti = "";

		// scandisco lettera per lettera la guessed word
		for (int i = 0; i < guessedWord.length(); i++) {
			// controllo se il carattere in posizione i-esima della guessed word è contenuto
			// nella secret word
			if (secretWord.contains("" + guessedWord.charAt(i)))
				// controllo se i caratteri i-esimi della gw e della sw sono uguali
				if (("" + guessedWord.charAt(i)).equals("" + secretWord.charAt(i)))
					stringa_suggerimenti = stringa_suggerimenti.concat("+"); // lettera indovinata e nella giusta
																				// posizione

				else {
					stringa_suggerimenti = stringa_suggerimenti.concat("?"); // lettera indovinata ma nella posizione
																				// sbagliata
				}

			else
				stringa_suggerimenti = stringa_suggerimenti.concat("X"); // lettera non indovinata
		}

		return stringa_suggerimenti;
	}

	/*
	 * metodo che traduce dall' inglese all' italiano la parola passata come
	 * parametro sfruttando il servizio indicato nel testo del progetto
	 * 
	 * @param word : an english word
	 * 
	 * @return a string that represents the italian translation of the string word
	 */
	public synchronized String Translate(String word) throws NullPointerException {
		// controllo che word non sia null
		if (word == null)
			throw new NullPointerException();
		String translation = null;
		try {
			// inserisco la parola da tradurre (word) nella query della URL http
			URL url = new URL("https://api.mymemory.translated.net/get?q=" + word + "!&langpair=en|it");
			// apro uno stream verso quella url,lo avvolgo prima in un InputStreamReader e
			// poi in un BufferedReader
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			// array di caratteri sufficientemente lungo in cui leggerò i caratteri della
			// risposta che mi arrivano sullo stream
			char[] array = new char[2048];
			// leggo i caratteri nell'array fino alla fine dello stream
			// -1 rappresenta l'end-of-the-stream
			while (in.read(array) != -1) {
				;
			}

			// converto l'array di caratteri in una stringa
			String response_json = new String(array);
			// converto la stringa in un oggetto JSON
			JSONObject obj = new JSONObject(response_json);
			// prende la traduzione cercando nei campi giusti della risposta
			translation = obj.getJSONObject("responseData").getString("translatedText");

		} catch (MalformedURLException exc) {
			exc.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return translation;
	}

	// genera e retituisce una parola random tra quelle presenti nel dizionario
	public synchronized String generateRandomWord() {
		// prendo un indice random nel range che va da 0 alla lunghezza del dizionario
		// -1
		int indice_random = (int) (Math.random() * (double) (this.words.size() - 1));
		// restituisco la parola del dizionario che sta in quella posizione
		return this.words.get(indice_random);
	}

	/**
	 * metodo che dato un Utente u e una String guessedWord testa o meno la vittoria
	 * del gioco, ovvero se la guessedWord proposta dall' Utente u corrisponde alla
	 * secret word
	 * 
	 * @param u           Utente who sends the guessedWord, bisogna controllare a
	 *                    priori che l' utente passato in input abbia chiesto di
	 *                    giocare al gioco corrente e, se questo è avvenuto, che non
	 *                    abbia raggiunto il limite massimo di tentativi disponibili
	 *                    per indovinare la parola segreta
	 * 
	 * @param guessedWord sent by user u, ricordarsi di controllare a priori che la
	 *                    parola sia della lunghezza giusta (10 caratteri )e che sia
	 *                    presenti nel dizionario
	 * 
	 * @return true <--> guessed , false <--> not guessed
	 */
	public synchronized boolean test(Utente u, String guessedWord)
			throws NullPointerException, NoSuchElementException, IllegalArgumentException {
		// faccio dei controlli in un' ottica di programmazione difensiva
		if (u == null || guessedWord == null)
			throw new NullPointerException();
		if (!players.containsKey(u))
			throw new NoSuchElementException(u.getUsername() + " non sta ancora partecipando alla sessione corrente");
		if (guessedWord.length() != 10)
			throw new IllegalArgumentException(guessedWord + " non è della lunghezza stabilita");
		if (!this.inVocaboulary(guessedWord))
			throw new IllegalArgumentException(guessedWord + " non è presente nel dizionario del gioco");
		// per prima cosa reperisco la partita p associata all' utente u
		Partita p = this.players.get(u);
		// se la partita non è finita  
		if (!p.isEnd()) {
		// aggiungo il tentativo alla lista dei tentativi della partita
		// memorizzando la stringa dei suggerimenti associata alla gw inviata	
		p.addTentativo(this.suggestions(guessedWord));
		// controllo se ho vinto
		if ( guessedWord.equals(secretWord)) { // se la gw e la sw sono uguali
			p.setEnd(true); // la partita è finita
			p.setWin(true); // ho vinto
		} else if (p.getNumTentativi() == 12) { // non ho indovinato controllo se ho esaurito i tentativi a disposizione
			p.setEnd(true); // la partita è finita
			p.setWin(false); // ho perso
		}
		if (p.isEnd()) { // se la partita è finita, vinta o persa
			// aggiorno le statistiche dell'utente
			u.getStatistiche().update(p.isWin(), p.getNumTentativi());
			// aggiorno il punteggio dell' utente
			u.updateScore();
			if (this.c.sort()) // aggiorno la classifica e se varia il podio
				try {
					ns.update(c.getPodium()); // faccio inviare una notifica dal servizio di notifica
				} catch (RemoteException e) {
					e.printStackTrace();
				}

		}
		}
		return p.isWin();
	}

	// metodo che termina abrupt una sessione di gioco
	// termina forzatamente le partite ancora in corso
	// considerandole perse
	public synchronized void endGame() {
		// tutte le partite non terminate vengono considerate perse
		for (Map.Entry<Utente, Partita> entry : this.players.entrySet()) { // scorro con un for generalizzato tutte le
																			// entry della map
			if (!entry.getValue().isEnd()) { // se la partita non è finita
				Utente u = entry.getKey();
				Partita p = entry.getValue();
				p.setEnd(true); // segno che è terminata
				p.setWin(false);// segno la partita come persa
				u.getStatistiche().update(false, p.getNumTentativi()); // aggiorno le statistiche
				u.updateScore();// aggiorno lo score
			}

		}

		// la classifica potrebbe essere cambiata, quindi la ordino
		if (this.c.sort()) // se è variato il podio invio le notifiche
			try {
				ns.update(c.getPodium());
			} catch (RemoteException e) {

				e.printStackTrace();
			}
	}

	// metodo che termina il match dell' utente u se è ancora in corso nell'attuale
	// sessione del gioco
	// throws NoSuchElementException se u non sta giocando alla sessione corrente
	// del gioco
	public synchronized void endMatch(Utente u) throws NullPointerException, NoSuchElementException {
		if (u == null)
			throw new NullPointerException();
		if (this.players.containsKey(u)) {
			Partita p = this.players.get(u);

			if (!p.isEnd()) {
				p.setEnd(true);
				p.setWin(false);
				u.getStatistiche().update(false, p.getNumTentativi());
				u.updateScore();
				if (this.c.sort())
					try {
						ns.update(c.getPodium());
					} catch (RemoteException e) {

						e.printStackTrace();
					}
			}
		} else
			throw new NoSuchElementException(u.getUsername() + " non sta ancora partecipando alla sessione corrente");
	}

}