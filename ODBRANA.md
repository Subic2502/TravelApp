# Planer putovanja – podsetnik za odbranu

## Tok aplikacije

1. **MainActivity** prikazuje listu sačuvanih putovanja (TripListFragment). Dugme „Novo putovanje“ otvara upitnik.
2. **QuestionnaireActivity** postavlja 8 pitanja, jedno po ekranu. Odgovori se čuvaju u nizu `answers`.
3. Na „Generiši plan“ pokreće se **PlanGenerationService** (foreground servis). On na pozadinskoj niti preko **GeminiService** šalje odgovore Gemini API-ju i dobija plan u JSON formatu.
4. Servis upisuje plan u SQLite bazu (**DatabaseHelper**) i prikazuje notifikaciju (**NotificationHelper**). Sve ovo radi i ako korisnik napusti aplikaciju.
5. Aplikacija otvara novi plan. Na telefonu se otvara TripDetailActivity, a na tabletu se plan prikazuje desno od liste.
6. U planu korisnik štiklira završene stavke, dodaje beleške i dodaje putovanje u kalendar (**CalendarHelper**).

---

## Obavezni koncepti

### 1. Dva layout-a (telefon i tablet), Fragmenti
- **Fajlovi:** `res/layout/activity_main.xml`, `res/layout-sw600dp/activity_main.xml`, `MainActivity.isTablet()`, `MainActivity.openTrip()`
- **Objašnjenje:** Android sam bira layout. Ako je najmanja širina ekrana bar 600dp, koristi fajl iz foldera `layout-sw600dp`. Samo tablet layout ima `detail_container`, pa `findViewById(R.id.detail_container) != null` znači da radimo na tabletu. Na tabletu `openTrip()` menja fragment u desnom delu ekrana (`replace`). Na telefonu pokreće `TripDetailActivity`, koja prikazuje isti `TripDetailFragment`.

### 2. Internet servis (Gemini REST API)
- **Fajl:** `GeminiService.java`, metode `generatePlan()`, `buildPrompt()`, `buildRequestBody()`, `sendRequest()`, `parseResponse()`
- **Objašnjenje:** Od odgovora iz upitnika pravimo tekst upita (prompt). Šaljemo ga POST zahtevom preko `HttpURLConnection`, a API ključ ide u zaglavlju `x-goog-api-key`. Sa `responseMimeType = application/json` tražimo od modela čist JSON. Odgovor parsiramo pomoću `org.json` putanjom `candidates[0].content.parts[0].text`. Ključ se čita iz `local.properties` u `BuildConfig.GEMINI_API_KEY`, pa nije upisan u kod.

### 3. Lokalna baza (SQLite)
- **Fajl:** `DatabaseHelper.java` (nasleđuje `SQLiteOpenHelper`)
- **Objašnjenje:** U `onCreate()` pravimo dve tabele, `trips` i `plan_items`. Stavka plana preko `trip_id` pokazuje na putovanje, sa `ON DELETE CASCADE`. U `onConfigure()` uključujemo strane ključeve, jer ih SQLite podrazumevano ne proverava. `insertTripWithItems()` upisuje putovanje i sve stavke u jednoj transakciji: ili se upiše sve, ili ništa. Postoje i metode `getAllTrips()`, `getTrip()`, `getPlanItems()`, `setItemDone()`, `updateItemNote()` i `deleteTrip()`.

### 4. Niti (threads)
- **Fajl:** `AppExecutor.java`. Koristi se u `PlanGenerationService.onStartCommand()`, `TripListFragment.loadTrips()` i `TripDetailFragment.loadTrip()`.
- **Objašnjenje:** U celoj aplikaciji postoji jedan `ExecutorService`, bazen od 4 pozadinske niti. Sav rad sa mrežom i bazom ide kroz `runInBackground()`. Samo glavna (UI) nit sme da menja prikaz. Zato rezultat vraćamo metodom `runOnMainThread()`, koja koristi `new Handler(Looper.getMainLooper())`. Pre prikaza rezultata proveravamo da li ekran još postoji (`isAdded()`, `isDestroyed()`).

### 5. Notifikacije
- **Fajl:** `NotificationHelper.java`, metode `createChannel()`, `createProgressNotification()`, `showPlanReady()`, `createOpenTripIntent()`. Dozvolu traži `MainActivity.requestNotificationPermission()`.
- **Objašnjenje:** Od Androida 8 svaka notifikacija mora pripadati kanalu, zato prvo pravimo kanal. Od Androida 13 dozvola `POST_NOTIFICATIONS` traži se u toku rada aplikacije. `PendingIntent` sistemu kaže šta da uradi na dodir: otvara `MainActivity` sa ID-jem putovanja, a ona prikazuje taj plan. Notifikaciju prikazuje servis kada sačuva plan, pa ona stiže i ako je korisnik napustio aplikaciju.

### 6. Content Provider (kalendar)
- **Fajl:** `CalendarHelper.java`, metode `addTripToCalendar()` i `findFirstCalendarId()`. Dozvole traži `TripDetailFragment.onCalendarClick()`.
- **Objašnjenje:** Kalendar je druga aplikacija, i svoje podatke deli preko Content Provider-a. Mi mu pristupamo preko `ContentResolver`-a. Prvo upitom nad `CalendarContract.Calendars` nalazimo ID prvog kalendara. Zatim metodom `insert(CalendarContract.Events.CONTENT_URI, values)` dodajemo celodnevni događaj „Putovanje: <destinacija>“. Potrebne su dozvole `READ_CALENDAR` i `WRITE_CALENDAR`. Ako kalendar ne postoji, prikazujemo Toast poruku.

### Dodatno: Foreground servis
- **Fajl:** `PlanGenerationService.java`, metode `start()`, `onStartCommand()`, `generateAndSavePlan()`, `setListener()`. Servis je prijavljen u `AndroidManifest.xml` (`foregroundServiceType="dataSync"`).
- **Objašnjenje:** Od Androida 15 aplikacija u pozadini gubi pristup internetu, a ubrzo je sistem i zamrzava. Običan pozadinski thread bi zato prekinuo poziv Gemini API-ju čim korisnik izađe iz aplikacije. Foreground servis kaže sistemu: „radim nešto važno“ i za to vreme prikazuje notifikaciju „Pravimo vaš plan…“. Zauzvrat ga sistem ne zamrzava i ne blokira mu mrežu. Kada završi, servis čuva rezultat i javlja ga aktivnosti preko interfejsa `Listener`, ako je ekran otvoren. Ako ekran nije otvoren, rezultat čeka dok se korisnik ne vrati.

---

## Ostale bitne stvari

- **Upitnik u jednoj aktivnosti:** pitanja su `List<Question>`. `showQuestion()` prikazuje samo ulaz koji odgovara tipu pitanja (TEXT, NUMBER, DATE ili CHOICE). Indeks, odgovori, datum i stanje učitavanja čuvaju se u `onSaveInstanceState()`, pa rotacija ne gubi napredak.
- **„Dan X“ u listi:** u `PlanItemAdapter.onBindViewHolder()` naslov dana je vidljiv samo kada se dan razlikuje od prethodne stavke.
- **Datumi:** čuvaju se kao milisekunde u UTC ponoći, jer ih tako vraća `MaterialDatePicker`.

---

## Moguća pitanja profesora

**1. Zašto ne smete mrežni poziv na glavnoj niti?**
Glavna nit crta ekran i obrađuje dodire. Mrežni poziv može trajati i desetine sekundi, pa bi aplikacija „zamrzla“ i Android bi prikazao ANR poruku. Android zato baca `NetworkOnMainThreadException` ako to pokušamo. Isto važi i za bazu: i ona je spor ulazno-izlazni posao.

**2. Kako aplikacija zna da je tablet?**
Ne proveravamo model uređaja. Android sam učitava `layout-sw600dp/activity_main.xml` kada je najmanja širina ekrana bar 600dp. Samo taj layout sadrži `detail_container`, pa `findViewById(R.id.detail_container) != null` znači da smo na tabletu.

**3. Šta je Content Provider?**
To je standardni Android mehanizam kojim jedna aplikacija deli podatke sa drugima preko URI adresa, na primer `CalendarContract.Events.CONTENT_URI`. Druge aplikacije mu pristupaju preko `ContentResolver`-a metodama `query`, `insert`, `update` i `delete`, slično bazi. Pristup štite dozvole.

**4. Čemu služi `Handler(Looper.getMainLooper())`?**
Looper glavne niti je red poruka koje glavna nit obrađuje jednu po jednu. Sa `handler.post(runnable)` stavljamo kod u taj red, pa se izvršava na glavnoj niti. Tako pozadinska nit bezbedno vraća rezultat za prikaz.

**5. Zašto transakcija u `insertTripWithItems()` i šta radi `ON DELETE CASCADE`?**
Putovanje i njegove stavke čine celinu. Ako bi upis pukao na pola, u bazi bi ostalo putovanje bez plana, a transakcija to sprečava. `ON DELETE CASCADE` znači da brisanje putovanja automatski briše i sve njegove stavke. Zato `deleteTrip()` briše samo jedan red.

**Dodatno: Zašto vam treba foreground servis, kad već imate ExecutorService?**
ExecutorService samo pravi pozadinsku nit unutar procesa aplikacije. Kada korisnik izađe iz aplikacije, Android tom procesu blokira internet i zamrzava ga, pa nit staje. Foreground servis drži proces „važnim“ dok posao ne završi. Nit iz ExecutorService-a i dalje radi sam posao, a servis samo obezbeđuje da je sistem ne zaustavi.

**Dodatno: Zašto se API ključ ne piše u kod?**
Kod ide na git, pa bi ključ bio javan. `local.properties` se ne šalje na git. Gradle pri build-u pravi `BuildConfig.GEMINI_API_KEY` iz tog fajla.
