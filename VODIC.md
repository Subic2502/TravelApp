# Vodič kroz aplikaciju „Planer putovanja“

Ovaj vodič je zamišljen kao čas uživo, samo napisan. Krećemo od trenutka kada korisnik dodirne ikonicu aplikacije i pratimo tok aplikacije, korak po korak, sve do kalendara. Usput objašnjavamo svaki Android pojam čim se prvi put pojavi.

**Kako da čitaš:**
- Čitaj redom. Svako poglavlje se oslanja na prethodno.
- Kod je prikazan u delovima, a ispod svakog dela objašnjeno je šta radi svaki red.
- Kad jedan deo koda poziva drugi, to je označeno ovako:

  > ➡️ **Poziva:** `NekaKlasa.nekaMetoda()` → objašnjeno u poglavlju X

- Na kraju svakog poglavlja su pitanja **„Proveri se“**. Pokušaj prvo sama da odgovoriš, pa tek onda otvori odgovor.
- Dok čitaš, korisno je da paralelno imaš otvoren projekat u Android Studiju i da pogledaš fajl o kome se priča.

**Razlika u odnosu na `ODBRANA.md`:** `ODBRANA.md` je kratak podsetnik za sam dan odbrane. Ovaj vodič objašnjava *zašto* je sve tako kako jeste.

---

## Sadržaj

0. [Osnove Androida pre nego što pogledamo kod](#0-osnove-androida-pre-nego-što-pogledamo-kod)
1. [Gde aplikacija počinje: AndroidManifest.xml](#1-gde-aplikacija-počinje-androidmanifestxml)
2. [Glavni ekran: MainActivity](#2-glavni-ekran-mainactivity)
3. [Lista putovanja: TripListFragment, TripAdapter i Trip](#3-lista-putovanja-triplistfragment-tripadapter-i-trip)
4. [Niti: AppExecutor](#4-niti-appexecutor)
5. [Lokalna baza: DatabaseHelper](#5-lokalna-baza-databasehelper)
6. [Upitnik: QuestionnaireActivity i Question](#6-upitnik-questionnaireactivity-i-question)
7. [Foreground servis: PlanGenerationService](#7-foreground-servis-plangenerationservice)
8. [Gemini API: GeminiService](#8-gemini-api-geminiservice)
9. [Notifikacije: NotificationHelper](#9-notifikacije-notificationhelper)
10. [Ekran plana: TripDetailActivity, TripDetailFragment, PlanItemAdapter](#10-ekran-plana-tripdetailactivity-tripdetailfragment-planitemadapter)
11. [Kalendar i Content Provider: CalendarHelper](#11-kalendar-i-content-provider-calendarhelper)
12. [Resursi, dizajn i build sistem](#12-resursi-dizajn-i-build-sistem)
13. [Kako pokrenuti aplikaciju i šta pokazati na odbrani](#13-kako-pokrenuti-aplikaciju-i-šta-pokazati-na-odbrani)

---

## 0. Osnove Androida pre nego što pogledamo kod

Pre prvog reda koda treba da znaš sedam pojmova. Sve ostalo se gradi na njima.

### 0.1 Kako izgleda projekat

```
TravelApp/
├── app/
│   ├── build.gradle.kts          ← podešavanja i biblioteke aplikacije
│   └── src/main/
│       ├── AndroidManifest.xml   ← "lična karta" aplikacije
│       ├── java/com/example/travelapp/   ← sav Java kod (16 klasa)
│       └── res/                  ← resursi: izgled, tekstovi, boje, ikonice
│           ├── layout/           ← XML izgledi ekrana (za telefon)
│           ├── layout-sw600dp/   ← XML izgled glavnog ekrana za tablet
│           ├── values/           ← strings.xml, colors.xml, themes.xml, dimens.xml
│           ├── drawable/         ← ikonice
│           └── color/            ← boje koje zavise od stanja (npr. izabrano / nije)
├── local.properties              ← tvoj Gemini API ključ (ne ide na git)
├── ODBRANA.md                    ← kratak podsetnik
└── VODIC.md                      ← ovaj vodič
```

Android aplikacija se uvek deli na dva sveta:
- **Java kod** (`java/...`) kaže *šta* aplikacija radi.
- **Resursi** (`res/...`) kažu *kako* aplikacija izgleda i *šta piše* na ekranu.

### 0.2 Activity: jedan ekran

**Activity** (aktivnost) je jedan ekran aplikacije. U Javi je to klasa koja nasleđuje `AppCompatActivity`. Naša aplikacija ima tri aktivnosti:
- `MainActivity`: lista putovanja,
- `QuestionnaireActivity`: upitnik,
- `TripDetailActivity`: plan putovanja (samo na telefonu).

Aktivnost ne pravimo sami sa `new MainActivity()`. Nju pravi **Android sistem**, a mi samo kažemo *koju* aktivnost želimo da otvorimo (pomoću `Intent`-a, vidi 0.5).

### 0.3 Životni ciklus (lifecycle)

Pošto aktivnost pravi i uništava sistem, on nas obaveštava o tome pozivanjem posebnih metoda. Te metode zovemo **callback metode životnog ciklusa**:

| Metoda | Kada je sistem poziva | Šta mi tu obično radimo |
|---|---|---|
| `onCreate()` | Aktivnost je napravljena | Postavljamo izgled ekrana i povezujemo dugmad |
| `onStart()` | Ekran postaje vidljiv | Počinjemo da slušamo događaje |
| `onResume()` | Korisnik može da dodiruje ekran | Osvežavamo podatke |
| `onPause()` | Korisnik odlazi (npr. otvara drugi ekran) | |
| `onStop()` | Ekran više nije vidljiv | Prestajemo da slušamo događaje |
| `onDestroy()` | Aktivnost se uništava | |

**Važno:** kada korisnik **rotira telefon**, Android *uništi* aktivnost i *napravi je ponovo* (`onDestroy` → `onCreate`), jer novi položaj ekrana može da traži drugačiji izgled. Zbog toga podaci koji su bili samo u promenljivama aktivnosti nestaju, osim ako ih sačuvamo. To ćemo videti u upitniku (poglavlje 6).

### 0.4 Layout, View i klasa `R`

- **View** je bilo koji element na ekranu: tekst (`TextView`), dugme (`Button`), polje za unos (`EditText`), lista (`RecyclerView`)…
- **Layout** je XML fajl koji opisuje koje View-ove ekran ima i kako su raspoređeni. Na primer, `res/layout/activity_main.xml`.
- **`R`** je klasa koju Android **sam generiše** iz resursa. Svaki resurs dobija broj (ID) u njoj:
  - `R.layout.activity_main` → layout fajl `activity_main.xml`
  - `R.id.fab_new_trip` → View kome smo u XML-u dali `android:id="@+id/fab_new_trip"`
  - `R.string.app_name` → tekst iz `strings.xml`

  Zato u Javi pišemo `findViewById(R.id.fab_new_trip)`: tako tražimo View sa tim ID-jem.

### 0.5 Intent: poruka sistemu

**Intent** je poruka kojom kažemo sistemu: „otvori ovaj ekran“ (ili „pokreni ovaj servis“). U Intent možemo da ubacimo i dodatne podatke (**extras**), na primer ID putovanja koje treba prikazati:

```java
Intent intent = new Intent(this, TripDetailActivity.class); // šta otvaramo
intent.putExtra("trip_id", 5);                              // dodatni podatak
startActivity(intent);                                      // pošalji sistemu
```

### 0.6 Context

**Context** je „pristup aplikaciji i sistemu“. Preko njega čitamo resurse (`getString(...)`), otvaramo bazu, pravimo notifikacije… Svaka aktivnost **jeste** Context (nasleđuje ga), pa u aktivnosti često prosleđujemo `this`. Postoji i `getApplicationContext()`: Context cele aplikacije, koji živi koliko i aplikacija, a ne koliko jedan ekran.

### 0.7 Fragment: deo ekrana

**Fragment** je deo ekrana koji ima svoj izgled i svoj kod, a živi *unutar* aktivnosti. Koristimo ga da bismo isti deo interfejsa mogli da postavimo na različita mesta. Na telefonu je lista putovanja ceo ekran, a na tabletu je lista levo, a plan desno, na **istom** ekranu. Lista i plan su zato fragmenti (`TripListFragment`, `TripDetailFragment`), a aktivnost odlučuje gde će ih postaviti.

### 0.8 Glavna nit (main / UI thread)

Svaka aplikacija ima jednu **glavnu nit**. Ona crta ekran i obrađuje dodire. Ako na njoj uradimo nešto sporo (mrežni poziv, rad sa bazom), ekran se „zamrzne“, a posle nekoliko sekundi Android prikaže poruku *„Aplikacija ne reaguje“* (ANR). Zato sav spor posao radimo na **pozadinskim nitima**. Pravilo glasi: **sporo → u pozadini; menjanje ekrana → samo na glavnoj niti.** O tome detaljno u poglavlju 4.

### Proveri se

1. Zašto ne pišemo `new MainActivity()`?
2. Šta se dešava sa aktivnošću kada korisnik rotira telefon?
3. Šta je `R.id.fab_new_trip` i odakle dolazi?

<details><summary>Odgovori</summary>

1. Aktivnosti pravi Android sistem. Mi samo šaljemo `Intent` u kome kažemo koju aktivnost želimo.
2. Android je uništi i napravi ponovo (`onDestroy` → `onCreate`). Podaci koji nisu sačuvani se gube.
3. To je broj (ID) koji je Android generisao u klasi `R` za View kome je u XML-u dat `android:id="@+id/fab_new_trip"`.

</details>

---

## 1. Gde aplikacija počinje: AndroidManifest.xml

📄 `app/src/main/AndroidManifest.xml`

Kada korisnik dodirne ikonicu, Android prvo čita **manifest**. To je „lična karta“ aplikacije: koje ekrane ima, koje dozvole traži i koji ekran je početni.

### 1.1 Dozvole

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

Aplikacija mora unapred da najavi šta sve želi da koristi:
- `INTERNET`: da bi mogla da pozove Gemini API. Ovu dozvolu korisnik ne mora da odobri, sistem je daje automatski.
- `POST_NOTIFICATIONS`: da bi mogla da prikazuje notifikacije. Od Androida 13 korisnik mora i **izričito** da je odobri dok aplikacija radi (poglavlje 2).
- `FOREGROUND_SERVICE` i `FOREGROUND_SERVICE_DATA_SYNC`: da bi mogla da pokrene foreground servis koji pravi plan (poglavlje 7).
- `READ_CALENDAR` i `WRITE_CALENDAR`: da bi mogla da upiše putovanje u kalendar. I ove korisnik mora izričito da odobri (poglavlje 11).

Dozvole koje korisnik mora da odobri zovemo **runtime dozvole** („u toku rada“). Nije dovoljno da stoje u manifestu, moramo ih i tražiti iz koda.

### 1.2 Aplikacija i ekrani

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:supportsRtl="true"
    android:theme="@style/Theme.TravelApp">
```

- `icon`: ikonica aplikacije.
- `label`: naziv ispod ikonice. `@string/app_name` znači „uzmi tekst iz `strings.xml` sa imenom `app_name`“, a to je „Planer putovanja“.
- `theme`: izgled cele aplikacije (boje, fontovi). Definisan je u `res/values/themes.xml` (poglavlje 12).

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Ovo je **najvažniji deo manifesta**:
- `android:name=".MainActivity"`: klasa `MainActivity`. Tačka na početku znači „u paketu aplikacije“ (`com.example.travelapp`).
- `exported="true"`: drugi delovi sistema, u ovom slučaju početni ekran telefona (launcher), smeju da otvore ovu aktivnost.
- `intent-filter` sa `MAIN` + `LAUNCHER` znači: **„ovo je početni ekran; napravi ikonicu za njega“**. Zato se, kad korisnik dodirne ikonicu, otvara baš `MainActivity`.

```xml
<activity android:name=".QuestionnaireActivity" android:exported="false" />
<activity android:name=".TripDetailActivity" android:exported="false" />

<service
    android:name=".PlanGenerationService"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

- Ostale dve aktivnosti imaju `exported="false"`. Mogu da ih otvore samo delovi naše aplikacije.
- **Svaka** aktivnost i svaki servis moraju biti prijavljeni u manifestu. Ako zaboravimo, aplikacija pukne kad pokuša da ih otvori.
- Servis je objašnjen u poglavlju 7. `foregroundServiceType="dataSync"` kaže sistemu kakav posao servis radi (razmena podataka sa serverom).

> ➡️ **Sledeće:** sistem pravi `MainActivity` i poziva njen `onCreate()` → poglavlje 2

### Proveri se

1. Kako Android zna koji ekran da otvori kada korisnik dodirne ikonicu?
2. Da li je dovoljno da `POST_NOTIFICATIONS` stoji u manifestu?

<details><summary>Odgovori</summary>

1. Po `intent-filter`-u sa `MAIN` i `LAUNCHER` koji `MainActivity` ima u manifestu.
2. Nije. Od Androida 13 to je runtime dozvola i moramo je tražiti iz koda (to radi `MainActivity.requestNotificationPermission()`).

</details>

---

## 2. Glavni ekran: MainActivity

📄 `MainActivity.java`

### 2.1 Deklaracija klase i konstante

```java
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "trip_id";

    private static final long NO_TRIP = -1;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1;
```

- `extends AppCompatActivity`: naša klasa **nasleđuje** Android-ovu klasu aktivnosti. Time dobija sve što aktivnost ume (prikaz ekrana, lifecycle…). Mi samo dopisujemo svoje.
- `EXTRA_TRIP_ID = "trip_id"`: ime („ključ“) pod kojim u Intent stavljamo ID putovanja. Konstanta je `public` jer je koriste i druge klase (notifikacija, upitnik, `TripDetailActivity`), pa svi moraju da koriste **isto** ime.
- `NO_TRIP = -1`: vrednost koja znači „nema putovanja“. ID-jevi u bazi su uvek pozitivni, pa -1 sigurno nije pravi ID.
- `NOTIFICATION_PERMISSION_REQUEST = 1`: broj koji identifikuje naš zahtev za dozvolu. Ovde nam nije bitan, ali metoda za traženje dozvole ga zahteva.
- `static final` znači konstanta: jedna vrednost za celu klasu, koja se nikad ne menja.

### 2.2 `onCreate`: šta se dešava kad se ekran napravi

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Android sam bira layout/activity_main.xml ili layout-sw600dp/activity_main.xml.
    setContentView(R.layout.activity_main);
```

- `@Override` znači da **prepisujemo** metodu iz roditeljske klase. Sistem će pozvati baš ovu našu verziju.
- `Bundle savedInstanceState` je „kesa“ sa sačuvanim stanjem. Kada se ekran pravi **prvi put**, ona je `null`. Kada se pravi **ponovo** (npr. posle rotacije), u njoj je ono što smo sačuvali.
- `super.onCreate(...)` prvo pušta roditeljsku klasu da uradi svoj deo posla. To je **obavezno**.
- `setContentView(R.layout.activity_main)` kaže: „ovaj ekran izgleda kako piše u `activity_main.xml`“.

**Ovde je ključ zadatka „dva layout-a“.** Postoje **dva** fajla sa istim imenom:
- `res/layout/activity_main.xml`: za telefon,
- `res/layout-sw600dp/activity_main.xml`: za tablet.

`sw600dp` znači *smallest width 600dp*: „koristi ovaj fajl ako je **najmanja** strana ekrana bar 600dp“. `dp` je jedinica koja ne zavisi od gustine piksela; 600dp je otprilike veličina malog tableta. Mi u kodu ne biramo ništa. **Android sam izabere pravi fajl.**

Pogledajmo ta dva fajla.

**Telefon** (`layout/activity_main.xml`), skraćeno:
```xml
<FrameLayout ... android:fitsSystemWindows="true">
    <LinearLayout android:orientation="vertical">
        <com.google.android.material.appbar.MaterialToolbar ... app:title="@string/app_name" />
        <androidx.fragment.app.FragmentContainerView
            android:id="@+id/list_container"
            android:name="com.example.travelapp.TripListFragment" ... />
    </LinearLayout>
    <ExtendedFloatingActionButton
        android:id="@+id/fab_new_trip"
        android:layout_gravity="bottom|end"
        android:text="@string/new_trip" ... />
</FrameLayout>
```

- `FrameLayout` slaže elemente jedan preko drugog. Zato dugme „Novo putovanje“ može da „lebdi“ preko liste, u donjem desnom uglu (`bottom|end`).
- `LinearLayout vertical` slaže elemente jedan ispod drugog: gore traka sa naslovom (`MaterialToolbar`), ispod lista.
- `FragmentContainerView` sa `android:name="...TripListFragment"` je **mesto za fragment**. Kada je navedeno `android:name`, Android **sam napravi** `TripListFragment` i postavi ga tu. Zato u `MainActivity` nigde ne piše `new TripListFragment()`.
- `fitsSystemWindows="true"`: novije verzije Androida crtaju aplikaciju „od ivice do ivice“, i ispod statusne trake (sat, baterija). Ovaj atribut dodaje razmak da sadržaj ne bi bio ispod nje.
- `ExtendedFloatingActionButton` je okruglo-izduženo „plutajuće“ dugme sa ikonicom i tekstom.

**Tablet** (`layout-sw600dp/activity_main.xml`), skraćeno:
```xml
<LinearLayout android:orientation="horizontal">
    <FrameLayout android:layout_width="0dp" android:layout_weight="4">
        <FragmentContainerView android:id="@+id/list_container"
            android:name="com.example.travelapp.TripListFragment" />
        <ExtendedFloatingActionButton android:id="@+id/fab_new_trip" ... />
    </FrameLayout>
    <View android:layout_width="1dp" ... />   <!-- tanka linija razdvajanja -->
    <FragmentContainerView
        android:id="@+id/detail_container"
        android:layout_width="0dp"
        android:layout_weight="6" />
</LinearLayout>
```

- `horizontal`: elementi idu jedan pored drugog.
- `layout_weight="4"` i `layout_weight="6"`: širina se deli u odnosu 4 : 6, to jest lista dobija **40%**, a detalji **60%** širine (`width="0dp"` znači „širinu odredi po težini“).
- `detail_container` **postoji samo u tablet verziji**. Prazan je dok korisnik ne izabere putovanje. Tada u njega ubacujemo `TripDetailFragment`.

Nastavljamo kroz `onCreate`:

```java
    findViewById(R.id.fab_new_trip).setOnClickListener(v ->
            startActivity(new Intent(this, QuestionnaireActivity.class)));
```

- `findViewById(R.id.fab_new_trip)` pronalazi dugme „Novo putovanje“ u layout-u.
- `setOnClickListener(...)` kaže: „kada korisnik dodirne dugme, uradi ovo“.
- `v -> ...` je **lambda**, kratak zapis funkcije. `v` je View koji je dodirnut (ovde ga ne koristimo), a posle strelice je ono što se izvršava.
- `new Intent(this, QuestionnaireActivity.class)` je poruka „otvori upitnik“. `this` je trenutna aktivnost (odakle), a `QuestionnaireActivity.class` je ekran koji otvaramo (kuda).
- `startActivity(...)` šalje Intent sistemu, i sistem otvara upitnik.

> ➡️ **Poziva:** `QuestionnaireActivity` → poglavlje 6

```java
    requestNotificationPermission();
    if (savedInstanceState == null) {
        openTripFromIntent(getIntent());
    }
}
```

- `requestNotificationPermission()` traži dozvolu za notifikacije (objašnjeno u 2.6).
- `getIntent()` vraća Intent kojim je **ova** aktivnost otvorena. Ako je otvorena dodirom na ikonicu, u njemu nema ID-ja putovanja. Ako je otvorena **dodirom na notifikaciju** „Vaš plan je spreman“, u njemu je ID putovanja, pa odmah otvaramo to putovanje.
- `if (savedInstanceState == null)`: ovo radimo **samo prvi put**. Bez ovog uslova bi se, posle svake rotacije, putovanje iz notifikacije otvaralo ponovo.

### 2.3 `onNewIntent`: aktivnost je već otvorena

```java
@Override
protected void onNewIntent(@NonNull Intent intent) {
    super.onNewIntent(intent);
    openTripFromIntent(intent);
}
```

Zamisli da je `MainActivity` već otvorena, a korisnik dodirne notifikaciju. Android tada **ne pravi** novu `MainActivity`, nego postojećoj pošalje novi Intent kroz `onNewIntent`. To se postiže „zastavicama“ (`FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`), koje ćemo videti u poglavljima 7 i 9. Mi tada samo otvorimo putovanje iz tog novog Intent-a.

### 2.4 Kako aplikacija zna da je na tabletu

```java
private boolean isTablet() {
    return findViewById(R.id.detail_container) != null;
}
```

Jednostavan trik: `detail_container` postoji **samo** u tablet layout-u. Ako ga `findViewById` pronađe, Android je učitao tablet verziju. Ako vrati `null`, učitao je telefonsku verziju. Ne gledamo veličinu ekrana ni model uređaja. **Pitamo layout koji je Android već izabrao.**

### 2.5 Otvaranje putovanja: telefon ili tablet

```java
public void openTrip(long tripId) {
    if (isTablet()) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_container, TripDetailFragment.newInstance(tripId))
                .commit();
    } else {
        Intent intent = new Intent(this, TripDetailActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        startActivity(intent);
    }
}
```

Ovo je srce **master-detail** obrasca („glavna lista – detalji“):

- **Tablet:**
  - `getSupportFragmentManager()` vraća upravljača fragmentima ove aktivnosti.
  - `beginTransaction()` započinje „transakciju“, to jest skup promena fragmenata.
  - `.replace(R.id.detail_container, ...)` kaže: „u desni deo ekrana stavi novi `TripDetailFragment`; ako je tu već neki, zameni ga“.
  - `TripDetailFragment.newInstance(tripId)` pravi fragment koji zna koje putovanje da prikaže (poglavlje 10).
  - `.commit()` primenjuje promene.
  - Rezultat: plan se pojavi desno, a lista ostaje levo. **Nema novog ekrana.**
- **Telefon:** nema mesta za dva dela ekrana, pa otvaramo **novi ekran** `TripDetailActivity` i u Intent stavljamo ID putovanja.

> ➡️ **Poziva:** `TripDetailFragment` / `TripDetailActivity` → poglavlje 10

```java
public void closeTripIfShown(long tripId) {
    Fragment detail = getSupportFragmentManager().findFragmentById(R.id.detail_container);
    if (detail instanceof TripDetailFragment
            && ((TripDetailFragment) detail).getTripId() == tripId) {
        getSupportFragmentManager().beginTransaction().remove(detail).commit();
    }
}
```

Ovu metodu poziva lista posle brisanja putovanja. Na tabletu je obrisano putovanje možda baš ono koje je prikazano desno, pa ga treba skloniti:
- `findFragmentById(R.id.detail_container)` vraća fragment koji je trenutno u desnom delu, ili `null`.
- `instanceof TripDetailFragment` proverava da li je to stvarno naš fragment. Ako je vrednost `null`, provera je `false`, pa radi i na telefonu, gde desnog dela uopšte nema.
- `((TripDetailFragment) detail).getTripId() == tripId` proverava da li prikazuje baš obrisano putovanje.
- Ako da, `remove(detail)` ga uklanja.

```java
private void openTripFromIntent(Intent intent) {
    long tripId = intent.getLongExtra(EXTRA_TRIP_ID, NO_TRIP);
    if (tripId != NO_TRIP) {
        openTrip(tripId);
    }
}
```

- `getLongExtra(EXTRA_TRIP_ID, NO_TRIP)` čita ID iz Intent-a. Ako ga nema, vraća podrazumevanu vrednost, `-1`.
- Ako ID postoji, otvaramo putovanje.

### 2.6 Traženje dozvole za notifikacije

```java
private void requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST);
    }
}
```

- `Build.VERSION.SDK_INT` je verzija Androida na uređaju. `TIRAMISU` je Android 13 (API 33). Pre Androida 13 ova dozvola nije postojala kao runtime dozvola, pa ništa ne tražimo.
- `checkSelfPermission(...)` proverava da li je dozvola već odobrena. Ako jeste, ne pitamo ponovo.
- `requestPermissions(...)` prikazuje sistemski dijalog „Dozvoliti aplikaciji da šalje obaveštenja?“.
- Odgovor korisnika ovde ne obrađujemo posebno. Ako odbije, aplikacija i dalje radi, samo neće prikazivati notifikacije. To se proverava u `NotificationHelper` (poglavlje 9).

### Proveri se

1. Gde u kodu piše „ako je tablet, učitaj tablet layout“?
2. Zašto `MainActivity` nigde ne pravi `new TripListFragment()`?
3. Šta je razlika između otvaranja putovanja na telefonu i na tabletu?
4. Zašto postoji `if (savedInstanceState == null)` oko `openTripFromIntent`?

<details><summary>Odgovori</summary>

1. Nigde, i to je poenta. Android sam bira `layout-sw600dp/activity_main.xml` ako je najmanja strana ekrana bar 600dp. Mi samo proveravamo rezultat preko `findViewById(R.id.detail_container) != null`.
2. Zato što je u XML-u `FragmentContainerView` sa `android:name="...TripListFragment"`, pa ga Android pravi sam.
3. Na tabletu fragment transakcijom menjamo desni deo istog ekrana (`replace`). Na telefonu otvaramo novu aktivnost `TripDetailActivity`.
4. Da se putovanje iz notifikacije ne bi ponovo otvaralo posle svake rotacije, jer se tada `onCreate` poziva ponovo.

</details>

---

## 3. Lista putovanja: TripListFragment, TripAdapter i Trip

Kada se `MainActivity` napravi, Android u `list_container` postavi `TripListFragment`. Sada gledamo šta taj fragment radi.

### 3.1 Model: klasa `Trip`

📄 `Trip.java`

Pre liste da vidimo **šta** prikazujemo. `Trip` je obična Java klasa (**model**) koja opisuje jedno putovanje:

```java
public class Trip {
    private final long id;
    private final String destination;
    private final int days;
    private final long startDate;
    private final String travelStyle;
    private final String budget;
    private final String summary;
    private final long createdAt;
    // konstruktor i getteri...
```

- Svako polje odgovara jednoj koloni tabele `trips` u bazi (poglavlje 5).
- `final` znači da se vrednost postavlja jednom, u konstruktoru, i posle se ne menja.
- `startDate` i `createdAt` su tipa `long`: vreme zapisano kao **broj milisekundi od 1.1.1970.** To je standardan način čuvanja datuma u programiranju.

Dve metode nisu običan getter:

```java
// Poslednji dan putovanja (putovanje od 3 dana traje od dana 1 do dana 3).
public long getEndDate() {
    return startDate + TimeUnit.DAYS.toMillis(days - 1);
}
```

Putovanje od 3 dana koje počinje 12.10. traje do 14.10., a ne do 15.10. Zato dodajemo `days - 1` dan. `TimeUnit.DAYS.toMillis(n)` pretvara n dana u milisekunde.

```java
// Datumi se čuvaju kao UTC ponoć (tako ih vraća MaterialDatePicker).
public static String formatDate(long millis) {
    SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    return format.format(new Date(millis));
}
```

- Pretvara milisekunde u tekst oblika `"12.10.2026"` (`DATE_PATTERN = "dd.MM.yyyy"`).
- `static` znači da ne treba objekat `Trip` da bi se pozvala: `Trip.formatDate(...)`.
- **Zašto UTC:** kalendar za izbor datuma (poglavlje 6) vraća datum kao ponoć po UTC vremenu, to jest po „svetskom“ vremenu bez vremenske zone. Ako bismo ga formatirali u lokalnoj zoni, u nekim zonama bi se prikazao **prethodni dan**. Zato i formatiramo u UTC-u.

### 3.2 `onCreateView`: pravljenje izgleda fragmenta

📄 `TripListFragment.java`

```java
public class TripListFragment extends Fragment implements TripAdapter.OnTripClickListener {

    private TripAdapter adapter;
    private TextView emptyText;
```

- `extends Fragment`: ovo je fragment.
- `implements TripAdapter.OnTripClickListener`: fragment „potpisuje ugovor“ (interfejs) da ume da obradi klik i dugi klik na putovanje. O tome u 3.6.

```java
@Override
public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                         @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_trip_list, container, false);
    emptyText = view.findViewById(R.id.empty_text);
```

- Aktivnost svoj izgled postavlja u `onCreate` sa `setContentView`. Fragment to radi u **`onCreateView`**, tako što izgled napravi i **vrati**.
- `inflater.inflate(R.layout.fragment_trip_list, container, false)` **naduvava** (inflate) XML: od opisa u XML-u pravi prave View objekte. `container` je roditelj u koji će fragment biti stavljen. `false` znači „nemoj ga sam ubaciti, to radi sistem“.
- `fragment_trip_list.xml` sadrži `RecyclerView` (lista) i `TextView` sa porukom „Još nemate sačuvanih putovanja…“, koja je podrazumevano skrivena (`visibility="gone"`).

```java
    adapter = new TripAdapter(this);
    RecyclerView recycler = view.findViewById(R.id.trips_recycler);
    recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
    recycler.setAdapter(adapter);
    return view;
}
```

Ovde moramo da objasnimo **RecyclerView**, jer je to najčešći način prikaza liste u Androidu.

**RecyclerView** prikazuje listu, ali je pametan: ako lista ima 100 stavki, a na ekran staje 6, on napravi samo ~8 View-ova. Kad korisnik skroluje, View koji izađe sa ekrana se **reciklira**: popuni se podacima nove stavke i ponovo pojavi. Otud i ime. To štedi memoriju i ubrzava rad.

RecyclerView-u trebaju dva pomoćnika:
1. **LayoutManager** kaže *kako* su stavke raspoređene. `LinearLayoutManager` znači jedna ispod druge, kao obična lista.
2. **Adapter** je „most“ između podataka (lista `Trip` objekata) i View-ova. Adapter zna koliko stavki ima, kako izgleda jedna stavka i kako da je popuni podacima. Naš adapter je `TripAdapter` (3.5).

- `new TripAdapter(this)`: prosleđujemo `this` (fragment) da bi adapter imao koga da obavesti kad korisnik dodirne stavku.
- `requireContext()` vraća Context fragmenta, odnosno aktivnosti u kojoj je. „require“ znači da baci grešku ako fragment trenutno nije u aktivnosti, ali u `onCreateView` uvek jeste.

### 3.3 `onResume`: učitavanje putovanja

```java
@Override
public void onResume() {
    super.onResume();
    loadTrips();
}
```

`onResume` se poziva **svaki put** kad korisnik može da koristi ekran: prvi put, ali i kad se vrati iz upitnika sa novim putovanjem. Zato listu učitavamo baš ovde, pa je uvek sveža.

```java
private void loadTrips() {
    DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
    AppExecutor.runInBackground(() -> {
        List<Trip> trips = db.getAllTrips();
        AppExecutor.runOnMainThread(() -> showTrips(trips));
    });
}
```

Ova četiri reda su **obrazac koji se ponavlja kroz celu aplikaciju**, pa ih pažljivo pročitaj:

1. `DatabaseHelper.getInstance(...)` uzima pristup bazi.
2. `AppExecutor.runInBackground(() -> { ... })`: sve unutar vitičastih zagrada izvršava se na **pozadinskoj niti**, jer je čitanje baze sporo.
3. `db.getAllTrips()` čita sva putovanja iz baze, na pozadinskoj niti.
4. `AppExecutor.runOnMainThread(() -> showTrips(trips))`: rezultat **vraćamo na glavnu nit**, jer samo ona sme da menja ekran.

> ➡️ **Poziva:** `AppExecutor` → poglavlje 4; `DatabaseHelper.getAllTrips()` → poglavlje 5

```java
private void showTrips(List<Trip> trips) {
    if (!isAdded()) {
        return;
    }
    adapter.setTrips(trips);
    emptyText.setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
}
```

- `isAdded()` proverava da li je fragment još uvek u aktivnosti. Dok je baza radila u pozadini, korisnik je možda već zatvorio ekran. Ako jeste, ne radimo ništa, inače bismo menjali ekran koji više ne postoji i aplikacija bi pukla.
- `adapter.setTrips(trips)` predaje nova putovanja adapteru, a on osveži listu.
- Poruka za praznu listu je vidljiva (`VISIBLE`) ako nema putovanja, a skrivena (`GONE`) ako ih ima. Zapis `uslov ? a : b` je **ternarni operator**: „ako je uslov tačan, a, inače b“.

### 3.4 Klik i dugi klik

```java
@Override
public void onTripClick(Trip trip) {
    ((MainActivity) requireActivity()).openTrip(trip.getId());
}
```

- `requireActivity()` vraća aktivnost u kojoj je fragment, a to je `MainActivity`.
- `(MainActivity)` je **kastovanje**: kažemo Javi „ovo je baš `MainActivity`“, da bismo mogli da pozovemo njenu metodu `openTrip`.
- Fragment **ne odlučuje** da li je telefon ili tablet. Samo kaže aktivnosti „otvori ovo putovanje“, a aktivnost zna kako (2.5).

```java
@Override
public void onTripLongClick(Trip trip) {
    new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_trip_title)
            .setMessage(getString(R.string.delete_trip_message, trip.getDestination()))
            .setPositiveButton(R.string.delete, (dialog, which) -> deleteTrip(trip.getId()))
            .setNegativeButton(R.string.cancel, null)
            .show();
}
```

Dugi pritisak prikazuje **dijalog za potvrdu**:
- `MaterialAlertDialogBuilder` gradi dijalog, deo po deo (ovaj stil zovemo *builder*).
- `setTitle` postavlja naslov „Brisanje putovanja“.
- `setMessage(getString(R.string.delete_trip_message, trip.getDestination()))`: tekst u `strings.xml` je `Da li želite da obrišete putovanje „%1$s“?`, a `%1$s` je mesto gde se ubacuje prvi parametar, ovde destinacija. Rezultat je „Da li želite da obrišete putovanje „Rim, Italija“?“.
- `setPositiveButton(R.string.delete, ...)`: dugme „Obriši“. Lambda se izvršava kad ga korisnik dodirne.
- `setNegativeButton(R.string.cancel, null)`: dugme „Otkaži“. `null` znači „samo zatvori dijalog“.
- `show()` prikazuje dijalog.

```java
private void deleteTrip(long tripId) {
    DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
    AppExecutor.runInBackground(() -> {
        db.deleteTrip(tripId);
        AppExecutor.runOnMainThread(() -> onTripDeleted(tripId));
    });
}

private void onTripDeleted(long tripId) {
    if (!isAdded()) {
        return;
    }
    ((MainActivity) requireActivity()).closeTripIfShown(tripId);
    loadTrips();
}
```

Isti obrazac kao u 3.3: brisanje se radi u pozadini, a na glavnoj niti onda:
1. kažemo aktivnosti da skloni plan sa desne strane ako je prikazan (2.5),
2. ponovo učitamo listu, sada bez obrisanog putovanja.

> ➡️ **Poziva:** `DatabaseHelper.deleteTrip()` → poglavlje 5

### 3.5 Adapter: `TripAdapter`

📄 `TripAdapter.java`

```java
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    public interface OnTripClickListener {
        void onTripClick(Trip trip);

        void onTripLongClick(Trip trip);
    }

    private final List<Trip> trips = new ArrayList<>();
    private final OnTripClickListener listener;

    public TripAdapter(OnTripClickListener listener) {
        this.listener = listener;
    }
```

- **Interfejs `OnTripClickListener`** je „ugovor“: ko god želi da sluša klikove, mora da ima ove dve metode. Adapter ne zna i ne mora da zna ko ga sluša (u našem slučaju fragment). Zna samo da taj neko ima `onTripClick` i `onTripLongClick`. Tako adapter ostaje nezavisan od ostatka aplikacije.
- `trips` je lista putovanja koju adapter prikazuje.
- `listener` je onaj ko sluša klikove, ovde `TripListFragment`.

```java
public void setTrips(List<Trip> newTrips) {
    trips.clear();
    trips.addAll(newTrips);
    notifyDataSetChanged();
}
```

Zamenjuje sadržaj liste i poziva `notifyDataSetChanged()`. Time kaže RecyclerView-u: „podaci su se promenili, iscrtaj ponovo“.

RecyclerView zatim poziva tri metode adaptera:

```java
@Override
public int getItemCount() {
    return trips.size();
}
```
1. **Koliko stavki ima?** Onoliko koliko ima putovanja.

```java
@Override
public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_trip, parent, false);
    return new TripViewHolder(view);
}
```
2. **Napravi novu (praznu) stavku.** Naduvavamo `item_trip.xml`, to jest jednu karticu sa tri teksta, i pakujemo je u **ViewHolder**. Ova metoda se poziva samo nekoliko puta, onoliko koliko kartica staje na ekran, jer se kartice posle recikliraju.

```java
@Override
public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
    Trip trip = trips.get(position);
    Context context = holder.itemView.getContext();

    holder.destination.setText(trip.getDestination());
    holder.dates.setText(context.getString(R.string.date_range,
            Trip.formatDate(trip.getStartDate()), Trip.formatDate(trip.getEndDate())));
    String days = context.getResources()
            .getQuantityString(R.plurals.days_count, trip.getDays(), trip.getDays());
    holder.info.setText(context.getString(R.string.trip_info,
            days, trip.getTravelStyle(), trip.getBudget()));
```
3. **Popuni stavku podacima putovanja na poziciji `position`.** Ova metoda se poziva stalno dok se skroluje:
   - Prvi red kartice je destinacija.
   - Drugi red su datumi. `date_range` je `%1$s – %2$s`, pa nastaje „12.10.2026 – 14.10.2026“.
   - Treći red: `getQuantityString(R.plurals.days_count, ...)` bira **pravilan oblik reči** prema broju. U `strings.xml` postoje oblici za „one“ (1 dan, 21 dan) i za „few“/„other“ (2 dana, 5 dana). Srpski ima različite oblike, a Android sam zna koji oblik ide uz koji broj. Zatim `trip_info` (`%1$s · %2$s · %3$s`) spaja tri dela: „3 dana · Kombinovano · Srednji“.

```java
    holder.itemView.setOnClickListener(v -> listener.onTripClick(trip));
    holder.itemView.setOnLongClickListener(v -> {
        listener.onTripLongClick(trip);
        return true;
    });
}
```

- Klik na karticu znači „javi slušaocu“. Slušalac je fragment, i on poziva `openTrip`.
- Dugi klik: `return true` znači „obradio sam ovaj dugi klik, nemoj dalje ništa da radiš“.

```java
static class TripViewHolder extends RecyclerView.ViewHolder {
    final TextView destination;
    final TextView dates;
    final TextView info;

    TripViewHolder(View itemView) {
        super(itemView);
        destination = itemView.findViewById(R.id.trip_destination);
        dates = itemView.findViewById(R.id.trip_dates);
        info = itemView.findViewById(R.id.trip_info);
    }
}
```

**ViewHolder** jednom pronađe View-ove u kartici (`findViewById` je relativno spor) i zapamti ih u poljima. Kad se kartica reciklira, `onBindViewHolder` samo koristi već pronađena polja.

### Proveri se

1. Zašto se lista učitava u `onResume`, a ne u `onCreateView`?
2. Koja su tri pitanja na koja adapter odgovara RecyclerView-u?
3. Zašto `showTrips` prvo proverava `isAdded()`?
4. Zašto se datumi formatiraju u UTC vremenskoj zoni?

<details><summary>Odgovori</summary>

1. Zato što se `onResume` poziva i kada se korisnik vrati na ekran (npr. posle pravljenja novog putovanja), pa je lista uvek osvežena.
2. Koliko ima stavki (`getItemCount`), kako napraviti praznu stavku (`onCreateViewHolder`) i kako je popuniti podacima (`onBindViewHolder`).
3. Zato što je učitavanje radilo u pozadini, a korisnik je u međuvremenu možda napustio ekran. Menjanje ekrana koji ne postoji bi srušilo aplikaciju.
4. Zato što ih `MaterialDatePicker` vraća kao ponoć po UTC-u. U lokalnoj zoni bi se negde prikazao prethodni dan.

</details>

---

## 4. Niti: AppExecutor

📄 `AppExecutor.java`

U prethodnom poglavlju smo stalno koristili `AppExecutor.runInBackground(...)` i `AppExecutor.runOnMainThread(...)`. Sada vidimo šta je u pozadini.

### 4.1 Problem

Iz 0.8 znamo da glavna nit crta ekran i da je ne smemo blokirati. Mrežni poziv ka Gemini-ju traje i do 20 sekundi, a ni čitanje baze nije trenutno. Ako bi se to radilo na glavnoj niti:
- ekran bi bio zamrznut,
- Android bi posle ~5 sekundi prikazao „Aplikacija ne reaguje“ (ANR),
- za mrežu je Android još strožiji: mrežni poziv na glavnoj niti **odmah** baca grešku `NetworkOnMainThreadException`.

### 4.2 Rešenje: cela klasa

```java
public final class AppExecutor {

    private static final int THREAD_COUNT = 4;

    private static final ExecutorService BACKGROUND = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AppExecutor() {
    }

    public static void runInBackground(Runnable task) {
        BACKGROUND.execute(task);
    }

    // Samo glavna nit sme da menja prikaz, zato rezultat vraćamo preko Handler-a.
    public static void runOnMainThread(Runnable task) {
        MAIN_HANDLER.post(task);
    }
}
```

Red po red:

- `final class` + `private AppExecutor() {}`: ovo je **pomoćna klasa** sa samo statičkim metodama. Privatni konstruktor sprečava da iko napravi njen objekat, jer za tim nema potrebe.
- **`ExecutorService`** je Javin „menadžer niti“. Umesto da za svaki posao sami pravimo novu nit (skupo), imamo **bazen (pool) od 4 niti** koje stalno čekaju posao:
  - `Executors.newFixedThreadPool(4)` pravi bazen od tačno 4 niti.
  - `BACKGROUND.execute(task)` predaje posao bazenu. Prva slobodna nit ga preuzme i izvrši.
  - Zašto 4, a ne 1: ako jedna nit čeka Gemini 20 sekundi, ostale tri i dalje mogu da čitaju bazu, pa lista ne čeka na Gemini.
  - U celoj aplikaciji postoji **jedan** `ExecutorService` (polje je `static`), i svi ga dele.
- **`Runnable`** je „komad koda koji može da se izvrši“. Lambda `() -> { ... }` koju prosleđujemo je upravo jedan Runnable.
- **`Looper`** je beskonačna petlja glavne niti: ona uzima „poruke“ iz reda jednu po jednu i izvršava ih (crtanje, dodiri…). `Looper.getMainLooper()` je looper glavne niti.
- **`Handler`** je „poštar“ koji stavlja posao u red nekog loopera. `new Handler(Looper.getMainLooper())` je poštar za glavnu nit.
- `MAIN_HANDLER.post(task)` stavlja `task` u red glavne niti. Kad dođe na red, izvršiće se **na glavnoj niti**, pa sme da menja ekran.

### 4.3 Kako to izgleda u praksi

```java
AppExecutor.runInBackground(() -> {                 // ← sada smo na POZADINSKOJ niti
    List<Trip> trips = db.getAllTrips();            //   spor posao: OK ovde
    AppExecutor.runOnMainThread(() -> {             // ← sada smo na GLAVNOJ niti
        showTrips(trips);                           //   menjanje ekrana: OK ovde
    });
});
```

Ovaj obrazac je svuda: lista, brisanje, ekran plana, beleške, kalendar, generisanje plana.

### Proveri se

1. Zašto bazen ima 4 niti, a ne 1?
2. Čemu služi `Handler(Looper.getMainLooper())`?
3. Šta bi se desilo kada bismo Gemini pozvali direktno iz `onClick` metode?

<details><summary>Odgovori</summary>

1. Da dugačak Gemini poziv ne bi blokirao čitanje baze. Ostale niti mogu da rade paralelno.
2. Da posao koji je završen u pozadini vrati na glavnu nit, jer samo ona sme da menja ekran.
3. `onClick` radi na glavnoj niti, pa bi Android odmah bacio `NetworkOnMainThreadException`. Čak i bez toga, ekran bi bio zamrznut dok poziv traje.

</details>

---

## 5. Lokalna baza: DatabaseHelper

📄 `DatabaseHelper.java`

### 5.1 Šta je SQLite i gde je baza

**SQLite** je mala relaciona baza ugrađena u svaki Android telefon. Nema servera; cela baza je **jedan fajl** u privatnom folderu aplikacije: `/data/data/com.example.travelapp/databases/travel_planner.db`. Druge aplikacije ne mogu da mu pristupe. Kad se aplikacija obriše, briše se i baza.

Sa bazom radimo pomoću klase **`SQLiteOpenHelper`** iz Androida. Ona se brine o otvaranju fajla i o tome da tabele postoje. Naša klasa je nasleđuje:

```java
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "travel_planner.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_TRIPS = "trips";
    private static final String TABLE_PLAN_ITEMS = "plan_items";
```

- `DATABASE_NAME` je ime fajla baze.
- `DATABASE_VERSION` je verzija šeme (strukture tabela). Ako bismo jednog dana menjali tabele, povećali bismo je na 2, i Android bi pozvao `onUpgrade` (5.3).

### 5.2 Tabele

```java
private static final String CREATE_TRIPS = "CREATE TABLE " + TABLE_TRIPS + " ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "destination TEXT, "
        + "days INTEGER, "
        + "start_date INTEGER, "
        + "travel_style TEXT, "
        + "budget TEXT, "
        + "summary TEXT, "
        + "created_at INTEGER)";
```

Ovo je običan SQL. Kada se Java stringovi spoje, dobija se:
```sql
CREATE TABLE trips (id INTEGER PRIMARY KEY AUTOINCREMENT, destination TEXT, days INTEGER, ...)
```
- `id INTEGER PRIMARY KEY AUTOINCREMENT`: jedinstveni broj svakog reda, koji baza sama dodeljuje (1, 2, 3…).
- `start_date` i `created_at` su `INTEGER`, jer čuvamo milisekunde (3.1).
- Kolone odgovaraju poljima klase `Trip`.

```java
private static final String CREATE_PLAN_ITEMS = "CREATE TABLE " + TABLE_PLAN_ITEMS + " ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "trip_id INTEGER REFERENCES " + TABLE_TRIPS + "(id) ON DELETE CASCADE, "
        + "day INTEGER, "
        + "time TEXT, "
        + "type TEXT, "
        + "title TEXT, "
        + "description TEXT, "
        + "done INTEGER DEFAULT 0, "
        + "note TEXT)";
```

Tabela `plan_items` sadrži stavke plana („09:00 – Koloseum“).
- `trip_id INTEGER REFERENCES trips(id)` je **strani ključ** (foreign key): svaka stavka pokazuje kom putovanju pripada. Jedno putovanje ima više stavki; to je veza **1 : N**.
- `ON DELETE CASCADE`: kada se obriše putovanje, baza **sama** briše sve njegove stavke. Zato metoda za brisanje (5.6) briše samo jedan red.
- `done INTEGER DEFAULT 0`: SQLite nema poseban tip za tačno/netačno, pa koristimo 0 (nije urađeno) i 1 (urađeno).
- `note TEXT`: lična beleška korisnika.

### 5.3 Singleton i lifecycle baze

```java
private static DatabaseHelper instance;

public static synchronized DatabaseHelper getInstance(Context context) {
    if (instance == null) {
        instance = new DatabaseHelper(context.getApplicationContext());
    }
    return instance;
}

private DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
}
```

- Ovo je obrazac **singleton**: u celoj aplikaciji postoji **samo jedan** `DatabaseHelper`. Prvi put kad ga neko traži, napravi se. Svaki sledeći put vraća se isti.
- Konstruktor je `private`, pa niko ne može da napravi drugi objekat. Mora da ide preko `getInstance`.
- `synchronized` znači da metodu može izvršavati samo jedna nit u isto vreme. Bez toga bi dve pozadinske niti, ako u istom trenutku prvi put pozovu `getInstance`, mogle da naprave dva objekta.
- `context.getApplicationContext()`: helper živi koliko i aplikacija, pa mu dajemo Context aplikacije, a ne nekog ekrana koji će biti uništen.
- Zašto jedna instanca: svi ekrani i niti dele istu konekciju ka bazi, pa nema sudaranja pri upisu.

```java
@Override
public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
}
```
SQLite **podrazumevano ne proverava** strane ključeve, pa bez ove linije `ON DELETE CASCADE` ne bi radio. `onConfigure` se poziva pri svakom otvaranju baze.

```java
@Override
public void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_TRIPS);
    db.execSQL(CREATE_PLAN_ITEMS);
}
```
Poziva se **samo jednom**: kad fajl baze još ne postoji (prvo korišćenje posle instalacije). `execSQL` izvršava SQL naredbu, i tako se prave dve tabele.

```java
@Override
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAN_ITEMS);
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
    onCreate(db);
}
```
Poziva se kad se `DATABASE_VERSION` poveća. Mi radimo najjednostavnije: obrišemo stare tabele i napravimo nove. Za studentski projekat je to u redu. Prava aplikacija bi prenela stare podatke.

### 5.4 Upis putovanja u jednoj transakciji

```java
public long insertTripWithItems(Trip trip, List<PlanItem> items) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
        long tripId = db.insert(TABLE_TRIPS, null, toValues(trip));
        for (PlanItem item : items) {
            db.insert(TABLE_PLAN_ITEMS, null, toValues(tripId, item));
        }
        db.setTransactionSuccessful();
        return tripId;
    } finally {
        db.endTransaction();
    }
}
```

Ovo je najvažnija metoda baze. Poziva je servis kada stigne plan od Gemini-ja (poglavlje 7).

- `getWritableDatabase()` otvara bazu za pisanje. Prvi put ovde se pozivaju `onConfigure` i `onCreate`.
- **Transakcija** je grupa operacija koje se izvrše ili **sve**, ili **nijedna**:
  - `beginTransaction()`: „počinjem grupu“.
  - `setTransactionSuccessful()`: „sve je prošlo kako treba, potvrdi“.
  - `endTransaction()` u `finally` bloku: ako je pre toga pozvan `setTransactionSuccessful`, izmene se trajno upisuju. Ako nije (npr. desila se greška usred petlje), **sve se poništava**.
  - Zašto: ako bi upis pukao posle putovanja, a pre stavki, u bazi bi ostalo putovanje bez plana. Transakcija to sprečava.
- `finally` se izvršava **uvek**, i kad je sve u redu i kad se desi greška.
- `db.insert(TABLE_TRIPS, null, toValues(trip))` ubacuje red i **vraća njegov novi `id`**. Taj `id` odmah koristimo kao `trip_id` za svaku stavku.

```java
private ContentValues toValues(Trip trip) {
    ContentValues values = new ContentValues();
    values.put("destination", trip.getDestination());
    values.put("days", trip.getDays());
    ...
    return values;
}
```

**`ContentValues`** je mapa „ime kolone → vrednost“. Android ga koristi za ubacivanje i menjanje redova, umesto da ručno pišemo `INSERT INTO ... VALUES (...)`. Prednost je i bezbednost: vrednosti se nikad ne lepe direktno u SQL tekst.

### 5.5 Čitanje: Cursor

```java
public List<Trip> getAllTrips() {
    List<Trip> trips = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().query(TABLE_TRIPS, null, null, null,
            null, null, "created_at DESC")) {
        while (cursor.moveToNext()) {
            trips.add(readTrip(cursor));
        }
    }
    return trips;
}
```

- `query(...)` je Android-ov način da se napiše `SELECT`. Parametri redom: tabela, kolone (`null` = sve), WHERE uslov, vrednosti za uslov, GROUP BY, HAVING, ORDER BY. Ovde to znači:
  ```sql
  SELECT * FROM trips ORDER BY created_at DESC
  ```
  (najnovija putovanja prva).
- Rezultat je **`Cursor`**: pokazivač koji ide red po red kroz rezultat.
  - `cursor.moveToNext()` prelazi na sledeći red i vraća `false` kad redova više nema. Zato je u `while` petlji.
- `try (Cursor cursor = ...) { }` je **try-with-resources**: kada blok završi, Java **automatski zatvara** cursor. Nezatvoren cursor drži memoriju.

```java
public Trip getTrip(long tripId) {
    try (Cursor cursor = getReadableDatabase().query(TABLE_TRIPS, null, "id = ?",
            new String[]{String.valueOf(tripId)}, null, null, null)) {
        return cursor.moveToFirst() ? readTrip(cursor) : null;
    }
}
```

- `"id = ?"` sa `new String[]{...}` daje SQL `WHERE id = 5`. Znak `?` je mesto za vrednost, a vrednost se šalje odvojeno. To je zaštita od **SQL injection** napada, jer vrednost nikad ne postaje deo SQL teksta.
- `moveToFirst()` vraća `false` ako red ne postoji (npr. putovanje je obrisano). Tada vraćamo `null`.

```java
public List<PlanItem> getPlanItems(long tripId) {
    ... query(TABLE_PLAN_ITEMS, null, "trip_id = ?", ..., "day, time") ...
}
```
To je `SELECT * FROM plan_items WHERE trip_id = ? ORDER BY day, time`: stavke jednog putovanja, poređane po danu, pa po vremenu. Vreme je tekst „09:00“, ali pošto uvek ima dve cifre, abecedni redosled je isti kao vremenski.

```java
private Trip readTrip(Cursor cursor) {
    return new Trip(
            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            cursor.getString(cursor.getColumnIndexOrThrow("destination")),
            ...);
}
```
Pretvara trenutni red cursora u `Trip` objekat. `getColumnIndexOrThrow("destination")` nalazi redni broj kolone po imenu, a `getString` / `getLong` / `getInt` čita vrednost tog tipa. Kod `PlanItem`-a: `cursor.getInt(...("done")) == 1` pretvara 0/1 u `false`/`true`.

### 5.6 Menjanje i brisanje

```java
public void setItemDone(long itemId, boolean done) {
    ContentValues values = new ContentValues();
    values.put("done", done ? 1 : 0);
    updatePlanItem(itemId, values);
}

public void updateItemNote(long itemId, String note) { ... values.put("note", note); ... }

private void updatePlanItem(long itemId, ContentValues values) {
    getWritableDatabase().update(TABLE_PLAN_ITEMS, values, "id = ?",
            new String[]{String.valueOf(itemId)});
}

public void deleteTrip(long tripId) {
    getWritableDatabase().delete(TABLE_TRIPS, "id = ?", new String[]{String.valueOf(tripId)});
}
```

- `update(...)` je `UPDATE plan_items SET done = 1 WHERE id = ?`.
- Obe metode koriste zajedničku `updatePlanItem`, da se kod ne bi ponavljao.
- `delete(...)` je `DELETE FROM trips WHERE id = ?`. Stavke se brišu same, zbog `ON DELETE CASCADE` + `onConfigure`.

### Proveri se

1. Šta radi `ON DELETE CASCADE` i zašto bez `onConfigure` ne bi radio?
2. Šta bi moglo da se desi bez transakcije u `insertTripWithItems`?
3. Zašto koristimo `"id = ?"` umesto da ID nalepimo direktno u SQL tekst?
4. Kada se poziva `onCreate` baze?

<details><summary>Odgovori</summary>

1. Kada se obriše putovanje, baza automatski briše njegove stavke. SQLite podrazumevano ne proverava strane ključeve, pa ih moramo uključiti u `onConfigure`.
2. Ako upis pukne na pola, ostalo bi putovanje bez plana (ili sa pola plana). Transakcija garantuje „sve ili ništa“.
3. Zbog bezbednosti (SQL injection) i ispravnosti: vrednost se šalje odvojeno od SQL naredbe.
4. Samo jednom, kada fajl baze još ne postoji (prvi upis/čitanje posle instalacije).

</details>

---

## 6. Upitnik: QuestionnaireActivity i Question

Korisnik je na glavnom ekranu dodirnuo „Novo putovanje“ (2.2). Otvara se `QuestionnaireActivity`.

### 6.1 Ideja: jedan ekran, osam pitanja

Umesto osam ekrana, imamo **jedan ekran** i **listu pitanja**. Kada korisnik ide napred ili nazad, menja se samo indeks trenutnog pitanja, a ekran prikazuje to pitanje. Ovo je jednostavnije za održavanje: dodavanje pitanja znači dodavanje jednog reda u listu.

### 6.2 Model: klasa `Question`

📄 `Question.java`

```java
public class Question {

    public enum Type { TEXT, NUMBER, DATE, CHOICE }

    private final Type type;
    private final String title;
    private final String description;
    private final String[] options;

    public Question(Type type, String title, String description) {
        this(type, title, description, new String[0]);
    }

    public Question(Type type, String title, String description, String[] options) { ... }
```

- **`enum`** je tip sa tačno određenim mogućim vrednostima. Pitanje može biti samo `TEXT` (slobodan tekst), `NUMBER` (broj), `DATE` (datum) ili `CHOICE` (izbor ponuđenih opcija).
- `options` su ponuđeni odgovori. Imaju ih samo `CHOICE` pitanja.
- Dva konstruktora: kraći je za pitanja bez opcija i poziva duži sa praznim nizom (`this(...)` poziva drugi konstruktor iste klase). Tako `options` nikad nije `null`.

### 6.3 Layout upitnika

📄 `res/layout/activity_questionnaire.xml`

Layout sadrži **sve moguće ulaze odjednom**, a kod prikazuje samo onaj koji trenutnom pitanju treba:

```
LinearLayout (vertikalno)
├── MaterialToolbar                 "Novo putovanje"
├── questionnaire_content           ← vidljiv dok se odgovara
│   ├── progress_text               "Pitanje 3 od 8"
│   ├── progress_indicator          traka napretka
│   ├── ScrollView
│   │   └── question_container      ← ovaj deo se animira
│   │       ├── question_title       "Datum polaska"
│   │       ├── question_description "Kada krećete na put?"
│   │       ├── answer_layout        polje za tekst/broj   (TEXT, NUMBER)
│   │       ├── date_button          dugme "Izaberite datum" (DATE)
│   │       └── choices_layout       prazan; kartice dodajemo iz koda (CHOICE)
│   └── dugmad: back_button "Nazad" | next_button "Dalje"
└── loading_layout                  ← vidljiv dok se pravi plan (skriven na početku)
    ├── CircularProgressIndicator   kružić koji se vrti
    └── "Pravimo vaš plan…"
```

`ScrollView` omogućava skrolovanje ako pitanje ne staje na ekran (npr. telefon položen horizontalno).

### 6.4 Polja klase

📄 `QuestionnaireActivity.java`

```java
public class QuestionnaireActivity extends AppCompatActivity
        implements PlanGenerationService.Listener {

    private static final String KEY_INDEX = "current_index";
    private static final String KEY_ANSWERS = "answers";
    private static final String KEY_START_DATE = "start_date";
    private static final String KEY_LOADING = "loading";
    private static final String DATE_PICKER_TAG = "date_picker";
    private static final long ANIMATION_DURATION_MS = 250;
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 14;

    private List<Question> questions;
    private String[] answers;
    private int currentIndex;
    private long startDateMillis;
    private boolean isLoading;
```

- `implements PlanGenerationService.Listener`: upitnik ume da primi rezultat od servisa koji pravi plan (poglavlje 7).
- `KEY_...` su imena pod kojima čuvamo stanje pri rotaciji (6.6).
- `questions` je lista 8 pitanja.
- `answers` je niz odgovora, na istom indeksu kao pitanje: `answers[0]` je destinacija, `answers[1]` broj dana… Svi odgovori su tekst, pa i datum („12.10.2026“).
- `currentIndex` je indeks pitanja koje je trenutno na ekranu (0 do 7).
- `startDateMillis` je izabrani datum **kao broj** milisekundi. Tekst „12.10.2026“ je za prikaz, a broj čuvamo za bazu i kalendar.
- `isLoading` govori da li je upitnik trenutno u stanju „Pravimo vaš plan…“.

Ispod toga su polja za View-ove (`progressText`, `answerInput`, `nextButton`…). Popunjava ih `bindViews()`.

### 6.5 `onCreate`

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_questionnaire);
    bindViews();

    questions = createQuestions();
    answers = new String[questions.size()];
    if (savedInstanceState != null) {
        currentIndex = savedInstanceState.getInt(KEY_INDEX);
        answers = savedInstanceState.getStringArray(KEY_ANSWERS);
        startDateMillis = savedInstanceState.getLong(KEY_START_DATE);
        isLoading = savedInstanceState.getBoolean(KEY_LOADING);
    }

    progressIndicator.setMax(questions.size());
    setupListeners();
    showQuestion();
    showLoading(isLoading);
}
```

1. Postavljamo izgled i pronalazimo sve View-ove (`bindViews` je niz `findViewById` poziva).
2. Pravimo listu pitanja i prazan niz odgovora: 8 mesta, sva `null`.
3. **Ako se ekran pravi ponovo** (posle rotacije), `savedInstanceState` nije `null`, pa vraćamo sačuvano: indeks, odgovore, datum i da li se plan pravi.
4. Traka napretka ima maksimum 8.
5. Povezujemo dugmad (`setupListeners`), prikazujemo trenutno pitanje i, ako treba, stanje učitavanja.

```java
private List<Question> createQuestions() {
    List<Question> list = new ArrayList<>();
    list.add(new Question(Question.Type.TEXT, getString(R.string.q_destination_title),
            getString(R.string.q_destination_description)));
    list.add(new Question(Question.Type.NUMBER, getString(R.string.q_days_title), ...));
    list.add(new Question(Question.Type.DATE, getString(R.string.q_date_title), ...));
    list.add(createChoiceQuestion(R.string.q_style_title, R.string.q_style_description,
            R.array.q_style_options));
    ... (još 4 CHOICE pitanja)
    return list;
}

private Question createChoiceQuestion(int titleRes, int descriptionRes, int optionsRes) {
    return new Question(Question.Type.CHOICE, getString(titleRes), getString(descriptionRes),
            getResources().getStringArray(optionsRes));
}
```

- Svi tekstovi dolaze iz `strings.xml` (`getString(R.string...)`), a ne iz koda. Tako tekst menjamo na jednom mestu, a aplikacija može lako da se prevede.
- Opcije su u `strings.xml` kao `<string-array>`, na primer:
  ```xml
  <string-array name="q_budget_options">
      <item>Nizak</item>
      <item>Srednji</item>
      <item>Visok</item>
  </string-array>
  ```
  `getResources().getStringArray(...)` ih čita kao Java niz.
- `createChoiceQuestion` je pomoćna metoda da se isti kod ne bi ponavljao pet puta.

### 6.6 Čuvanje stanja pri rotaciji

```java
@Override
protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_INDEX, currentIndex);
    outState.putStringArray(KEY_ANSWERS, answers);
    outState.putLong(KEY_START_DATE, startDateMillis);
    outState.putBoolean(KEY_LOADING, isLoading);
}
```

Iz 0.3 znamo da rotacija uništava i ponovo pravi aktivnost. **Pre** uništavanja Android poziva `onSaveInstanceState` i daje nam `Bundle` (kesu ključ → vrednost). Šta stavimo u nju, dobijamo nazad u `onCreate(savedInstanceState)` nove aktivnosti (6.5). Rezultat: korisnik na 5. pitanju rotira telefon i ostaje na 5. pitanju, sa svim odgovorima.

### 6.7 Dugmad i unos teksta

```java
private void setupListeners() {
    backButton.setOnClickListener(v -> goToQuestion(currentIndex - 1));
    nextButton.setOnClickListener(v -> onNextClick());
    dateButton.setOnClickListener(v -> showDatePicker());
    answerInput.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            answers[currentIndex] = s.toString().trim();
            updateNextButton();
        }
    });
}
```

- „Nazad“ ide na prethodno pitanje, a „Dalje“ na sledeće ili, na kraju, pokreće pravljenje plana.
- **`TextWatcher`** sluša svaku promenu teksta u polju. Interfejs ima tri metode, ali nama treba samo `afterTextChanged`, pa su ostale dve prazne (moraju postojati jer to interfejs traži).
- Posle svake promene upisujemo tekst kao odgovor na trenutno pitanje. `trim()` uklanja razmake sa početka i kraja. Zatim proveravamo da li „Dalje“ sme da bude aktivno.

```java
private void onNextClick() {
    if (currentIndex == questions.size() - 1) {
        startGeneration();
    } else {
        goToQuestion(currentIndex + 1);
    }
}

private void goToQuestion(int newIndex) {
    boolean forward = newIndex > currentIndex;
    currentIndex = newIndex;
    hideKeyboard();
    showQuestion();
    animateQuestion(forward);
}
```

- Ako smo na poslednjem pitanju (indeks 7), „Dalje“ (koje tada piše „Generiši plan“) pokreće pravljenje plana. Inače ide na sledeće pitanje.
- `goToQuestion` pamti smer (napred ili nazad, zbog animacije), menja indeks, sklanja tastaturu, prikazuje novo pitanje i animira ga.

> ➡️ **Poziva:** `startGeneration()` → 6.11 i poglavlje 7

### 6.8 Prikaz pitanja

```java
private void showQuestion() {
    Question question = questions.get(currentIndex);
    progressText.setText(getString(R.string.question_progress,
            currentIndex + 1, questions.size()));
    progressIndicator.setProgressCompat(currentIndex + 1, true);
    titleText.setText(question.getTitle());
    descriptionText.setText(question.getDescription());
```

- `question_progress` je `Pitanje %1$d od %2$d`. `%d` je mesto za ceo broj, pa nastaje „Pitanje 3 od 8“. Dodajemo 1 jer indeksi počinju od 0, a ljudi broje od 1.
- `setProgressCompat(..., true)` pomera traku napretka, sa animacijom (`true`).

```java
    Question.Type type = question.getType();
    boolean isTextInput = type == Question.Type.TEXT || type == Question.Type.NUMBER;
    answerLayout.setVisibility(isTextInput ? View.VISIBLE : View.GONE);
    dateButton.setVisibility(type == Question.Type.DATE ? View.VISIBLE : View.GONE);
    choicesLayout.setVisibility(type == Question.Type.CHOICE ? View.VISIBLE : View.GONE);
```

Ovo je trik „jednog layout-a“: od tri moguća ulaza prikazujemo samo onaj koji odgovara tipu pitanja. `GONE` znači da je View skriven i **ne zauzima prostor**. Postoji i `INVISIBLE`: skriven, ali zauzima prostor.

```java
    if (isTextInput) {
        showTextInput(type);
    } else if (type == Question.Type.DATE) {
        showDateAnswer();
    } else {
        showChoices(question);
    }

    backButton.setEnabled(currentIndex > 0);
    boolean isLast = currentIndex == questions.size() - 1;
    nextButton.setText(isLast ? R.string.generate_plan : R.string.next);
    updateNextButton();
}
```

- Popunjavamo vidljivi ulaz prethodnim odgovorom, ako postoji (kad se korisnik vrati nazad, vidi šta je već uneo).
- „Nazad“ je isključeno na prvom pitanju.
- Na poslednjem pitanju dugme piše „Generiši plan“, a inače „Dalje“.

### 6.9 Tri vrste unosa

**Tekst i broj:**
```java
private void showTextInput(Question.Type type) {
    answerInput.setInputType(type == Question.Type.NUMBER
            ? InputType.TYPE_CLASS_NUMBER
            : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    String answer = answers[currentIndex];
    answerInput.setText(answer == null ? "" : answer);
    answerInput.setSelection(answerInput.length());
}
```
- `setInputType` bira koju tastaturu telefon prikazuje: za broj samo cifre, za tekst slova, sa velikim početnim slovom svake reči (`CAP_WORDS`), zgodno za „Rim, Italija“.
- U polje upisujemo prethodni odgovor (ili prazno), a `setSelection(length)` stavlja kursor na kraj teksta.

**Izbor ponuđenih opcija (velike kartice):**
```java
private void showChoices(Question question) {
    choicesLayout.removeAllViews();
    for (String option : question.getOptions()) {
        MaterialCardView card = (MaterialCardView) getLayoutInflater()
                .inflate(R.layout.item_choice, choicesLayout, false);
        TextView optionText = card.findViewById(R.id.choice_text);
        optionText.setText(option);
        card.setChecked(option.equals(answers[currentIndex]));
        card.setOnClickListener(v -> selectChoice(question, option));
        choicesLayout.addView(card);
    }
}

private void selectChoice(Question question, String option) {
    answers[currentIndex] = option;
    showChoices(question);
    updateNextButton();
}
```
- `removeAllViews()` briše kartice prethodnog pitanja.
- Za svaku opciju naduvavamo `item_choice.xml` (velika `MaterialCardView` kartica) i upisujemo tekst opcije.
- `setChecked(...)` označava karticu ako je ta opcija već izabrana. Označena kartica dobija ivicu u boji aplikacije i kvačicu. Boja ivice je definisana u `res/color/choice_stroke.xml` kao **selektor**: jedna boja za stanje `checked`, druga za ostala stanja.
- `addView(card)` dodaje karticu na ekran.
- Kada korisnik dodirne karticu, `selectChoice` upiše odgovor i ponovo iscrta kartice, da bi kvačica prešla na novu.

**Datum:**
```java
private void showDatePicker() {
    CalendarConstraints constraints = new CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build();
    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.q_date_title)
            .setCalendarConstraints(constraints)
            .setSelection(startDateMillis > 0
                    ? startDateMillis : MaterialDatePicker.todayInUtcMilliseconds())
            .build();
    picker.addOnPositiveButtonClickListener(this::onDateSelected);
    picker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
}
```
- `MaterialDatePicker` je gotov kalendar-dijalog iz Material biblioteke.
- `DateValidatorPointForward.now()` dozvoljava samo datume od **danas nadalje**. Prošli dani su sivi i ne mogu da se izaberu.
- `setSelection(...)` određuje koji je dan označen kad se dijalog otvori: već izabrani datum, ili danas.
- `this::onDateSelected` je **referenca na metodu**, kraći zapis za lambdu `selection -> onDateSelected(selection)`. Poziva se kad korisnik pritisne „OK“.
- `picker.show(...)`: dijalog je zapravo fragment, pa ga prikazujemo preko FragmentManager-a.

```java
private void onDateSelected(Long selection) {
    startDateMillis = selection;
    answers[currentIndex] = Trip.formatDate(selection);
    showDateAnswer();
    updateNextButton();
}
```
Pamtimo datum kao broj (za bazu) i kao tekst (za odgovor i prikaz na dugmetu).

### 6.10 Kada sme „Dalje“

```java
private void updateNextButton() {
    nextButton.setEnabled(isAnswered(currentIndex));
}

private boolean isAnswered(int index) {
    String answer = answers[index];
    if (answer == null || answer.isEmpty()) {
        return false;
    }
    if (questions.get(index).getType() == Question.Type.NUMBER) {
        int days = parseDays(answer);
        return days >= MIN_DAYS && days <= MAX_DAYS;
    }
    return true;
}

private int parseDays(String answer) {
    try {
        return Integer.parseInt(answer);
    } catch (NumberFormatException e) {
        return 0;
    }
}
```

- „Dalje“ je isključeno dok pitanje nema odgovor.
- Za broj dana važi dodatno pravilo: od 1 do 14.
- `Integer.parseInt` pretvara tekst u broj. Ako tekst nije ispravan broj (npr. preveliki broj koji ne staje u `int`), baca `NumberFormatException`. Hvatamo je i vraćamo 0, a 0 nije u dozvoljenom opsegu.

### 6.11 Animacija i tastatura

```java
private void animateQuestion(boolean forward) {
    float offset = getResources().getDimension(R.dimen.question_slide_offset);
    questionContainer.setAlpha(0f);
    questionContainer.setTranslationX(forward ? offset : -offset);
    questionContainer.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(ANIMATION_DURATION_MS)
            .start();
}
```
- Pitanje odjednom postane potpuno prozirno (`alpha 0`) i pomereno udesno, ili ulevo ako idemo nazad (`translationX`). Pomeraj je 48dp, iz `dimens.xml`.
- `animate()` ga za 250 ms vraća na mesto i čini potpuno vidljivim. Rezultat je kratak efekat „klizanja i pojavljivanja“.

```java
private void hideKeyboard() {
    InputMethodManager imm = getSystemService(InputMethodManager.class);
    imm.hideSoftInputFromWindow(answerInput.getWindowToken(), 0);
}
```
Kad se pređe sa tekstualnog pitanja na pitanje sa karticama, tastatura bi ostala otvorena. `InputMethodManager` je sistemski servis za tastaturu, i ovde je zatvaramo.

### 6.12 Pokretanje pravljenja plana

```java
private void startGeneration() {
    showLoading(true);
    PlanGenerationService.start(this, getQuestionTitles(), answers.clone(), startDateMillis);
    PlanGenerationService.setListener(this);
}

private String[] getQuestionTitles() {
    String[] titles = new String[questions.size()];
    for (int i = 0; i < questions.size(); i++) {
        titles[i] = questions.get(i).getTitle();
    }
    return titles;
}

private void showLoading(boolean loading) {
    isLoading = loading;
    contentLayout.setVisibility(loading ? View.GONE : View.VISIBLE);
    loadingLayout.setVisibility(loading ? View.VISIBLE : View.GONE);
}
```

1. `showLoading(true)` sakriva pitanja i prikazuje kružić „Pravimo vaš plan…“.
2. `PlanGenerationService.start(...)` pokreće servis i šalje mu:
   - naslove pitanja („Destinacija“, „Broj dana“…), koji su potrebni za tekst upita ka Gemini-ju,
   - **kopiju** odgovora (`answers.clone()`), da servis ima svoju verziju koju ništa ne može da promeni,
   - datum polaska.
3. `setListener(this)` znači: „javi mi kad budeš gotov“.

> ➡️ **Poziva:** `PlanGenerationService` → poglavlje 7

Rezultat stiže u jednu od dve metode iz interfejsa `Listener`:

```java
@Override
public void onPlanReady(long tripId) {
    Intent intent = new Intent(this, MainActivity.class)
            .putExtra(MainActivity.EXTRA_TRIP_ID, tripId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(intent);
    finish();
}
```
**Uspeh:** otvaramo `MainActivity` sa ID-jem novog putovanja.
- `FLAG_ACTIVITY_CLEAR_TOP`: ako `MainActivity` već postoji ispod upitnika (a postoji, jer smo iz nje došli), ne pravi novu, nego vrati postojeću i ukloni sve iznad nje.
- `FLAG_ACTIVITY_SINGLE_TOP`: postojeća `MainActivity` dobija Intent kroz `onNewIntent` (2.3), umesto da se pravi ponovo.
- `finish()` zatvara upitnik.
- `MainActivity.onNewIntent` → `openTripFromIntent` → `openTrip`: novi plan se otvara (na telefonu kao poseban ekran, na tabletu desno).

```java
@Override
public void onPlanFailed(int messageRes) {
    showLoading(false);
    showError(messageRes);
}

private void showError(int messageRes) {
    new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error_title)
            .setMessage(messageRes)
            .setCancelable(false)
            .setPositiveButton(R.string.try_again, (dialog, which) -> startGeneration())
            .setNegativeButton(R.string.cancel, null)
            .show();
}
```
**Greška** (nema interneta, server je zauzet, neispravan odgovor): vraćamo pitanja i prikazujemo dijalog „Greška“ sa dugmadima „Pokušaj ponovo“ i „Otkaži“. `setCancelable(false)` znači da korisnik mora da izabere jedno od dva dugmeta; dodir pored dijaloga ga ne zatvara. Odgovori su sačuvani, pa „Pokušaj ponovo“ odmah pokreće novo pravljenje.

### 6.13 `onStart` i `onStop`: kada slušamo servis

```java
@Override
protected void onStart() {
    super.onStart();
    if (isLoading) {
        PlanGenerationService.setListener(this);
    }
}

@Override
protected void onStop() {
    super.onStop();
    PlanGenerationService.setListener(null);
}
```
- Kada ekran **nije vidljiv** (`onStop`: korisnik je izašao iz aplikacije ili je ekran uništen pri rotaciji), prestajemo da slušamo. Ne treba otvarati ekrane ili dijaloge kad korisnik nije tu.
- Kada se ekran **ponovo vidi** (`onStart`), a plan se još pravi ili je u međuvremenu gotov, ponovo slušamo. Ako je rezultat stigao dok nas nije bilo, servis nam ga odmah predaje (7.5).

### Proveri se

1. Kako jedan layout prikazuje četiri različita tipa pitanja?
2. Šta bi se desilo pri rotaciji da nema `onSaveInstanceState`?
3. Zašto čuvamo datum i kao broj (`startDateMillis`) i kao tekst?
4. Kako „Dalje“ zna da li sme da bude aktivno?
5. Šta rade `CLEAR_TOP` i `SINGLE_TOP` u `onPlanReady`?

<details><summary>Odgovori</summary>

1. Layout ima sve ulaze (polje za tekst, dugme za datum, prostor za kartice). `showQuestion` prikazuje (`VISIBLE`) samo onaj koji odgovara tipu, a ostale skriva (`GONE`).
2. Aktivnost bi se napravila ispočetka: korisnik bi se vratio na prvo pitanje bez odgovora.
3. Tekst („12.10.2026“) je odgovor za Gemini i prikaz na dugmetu. Broj je za bazu i kalendar, gde se računa sa datumima.
4. Posle svake promene odgovora poziva se `updateNextButton()`, koja preko `isAnswered()` proverava da li odgovor postoji i, za broj dana, da li je od 1 do 14.
5. Vraćaju postojeću `MainActivity` (umesto pravljenja nove), uklanjaju ekrane iznad nje i predaju joj Intent kroz `onNewIntent`.

</details>

---

## 7. Foreground servis: PlanGenerationService

📄 `PlanGenerationService.java`

### 7.1 Zašto servis, a ne samo pozadinska nit?

Ovo je pitanje koje profesor može da postavi, pa ga objašnjavamo pažljivo.

Pozadinska nit iz `AppExecutor`-a radi **unutar procesa aplikacije**. Dok je aplikacija na ekranu, sve je u redu. Ali novije verzije Androida (to smo proverili na Androidu 16) prema aplikaciji koja ode u pozadinu (korisnik pritisne Home ili prevuče aplikaciju) rade sledeće:
1. posle kratkog vremena joj **blokiraju internet**,
2. ubrzo je **zamrznu** (proces prestane da dobija procesorsko vreme),
3. ako korisnik aplikaciju potpuno zatvori, **ugase** proces, a sa njim i sve niti.

Kod nas to znači: korisnik pokrene pravljenje plana, izađe iz aplikacije, i Gemini poziv pukne. Plan se ne sačuva i notifikacija ne stigne.

**Service** (servis) je Android komponenta za posao **bez ekrana**. **Foreground servis** je servis koji sistemu kaže: „radim nešto što je korisniku važno i on zna za to“. Dokaz je **notifikacija koja stalno stoji** dok servis radi („Pravimo vaš plan…“). Zauzvrat sistem tom procesu **ne blokira internet i ne zamrzava ga**.

Važno: servis **ne zamenjuje** `AppExecutor`. Sam posao i dalje radi pozadinska nit iz `AppExecutor`-a. Servis samo obezbeđuje da sistem tu nit ne prekine. Zašto nit, a ne sam servis? Zato što i servis radi na **glavnoj** niti, pa bi mrežni poziv u njemu opet pukao.

Servis je prijavljen u manifestu (1.2), sa dozvolama `FOREGROUND_SERVICE` i `FOREGROUND_SERVICE_DATA_SYNC`.

### 7.2 Konstante i statička polja

```java
public class PlanGenerationService extends Service {

    public interface Listener {
        void onPlanReady(long tripId);

        void onPlanFailed(int messageRes);
    }

    private static final String EXTRA_QUESTION_TITLES = "question_titles";
    private static final String EXTRA_ANSWERS = "answers";
    private static final String EXTRA_START_DATE = "start_date";
    private static final long NO_TRIP = -1;
    private static final int NO_ERROR = 0;

    // Redni brojevi pitanja čiji se odgovori čuvaju u bazi.
    private static final int Q_DESTINATION = 0;
    private static final int Q_DAYS = 1;
    private static final int Q_STYLE = 3;
    private static final int Q_BUDGET = 4;

    private static Listener listener;
    private static long pendingTripId = NO_TRIP;
    private static int pendingErrorRes = NO_ERROR;
```

- `extends Service`: ovo je servis.
- **Interfejs `Listener`** je ugovor za onoga ko čeka rezultat (upitnik). Dva moguća ishoda su uspeh (ID novog putovanja) ili greška (ID poruke iz `strings.xml`, npr. `R.string.error_network`).
- `EXTRA_...` su ključevi za podatke u Intent-u.
- `Q_DESTINATION = 0`… su indeksi pitanja čije odgovore upisujemo u tabelu `trips`. Odgovore na pitanja o hrani, smeštaju i društvu ne čuvamo posebno; oni služe samo kao ulaz za Gemini.
- `listener` je ko trenutno sluša (upitnik, ili `null`).
- `pendingTripId` / `pendingErrorRes` su **rezultat koji čeka** da ga neko preuzme. Ako je korisnik van aplikacije kad plan bude gotov, nema ko da ga primi, pa ga čuvamo dok se upitnik ne vrati.
- Polja su `static` jer upitnik i servis nisu isti objekat, a moraju da dele ove vrednosti. **Sva ova polja menjamo samo na glavnoj niti**, pa nema opasnosti da ih dve niti menjaju u isto vreme.

### 7.3 Pokretanje: `start`

```java
public static void start(Context context, String[] questionTitles, String[] answers,
                         long startDate) {
    pendingTripId = NO_TRIP;
    pendingErrorRes = NO_ERROR;
    Intent intent = new Intent(context, PlanGenerationService.class)
            .putExtra(EXTRA_QUESTION_TITLES, questionTitles)
            .putExtra(EXTRA_ANSWERS, answers)
            .putExtra(EXTRA_START_DATE, startDate);
    ContextCompat.startForegroundService(context, intent);
}
```

- Prvo brišemo eventualni stari rezultat, da ga novi slušalac ne bi greškom primio.
- Pravimo Intent ka servisu i u njega pakujemo podatke. Servis je, kao i aktivnost, pravi sistem, pa podatke ne možemo da mu prosledimo kroz konstruktor. Zato idu kroz Intent.
- `startForegroundService(...)` traži od sistema da pokrene servis kao foreground servis. Sistem napravi servis i pozove njegov `onStartCommand`.

### 7.4 `onStartCommand`: servis je pokrenut

```java
@SuppressLint("InlinedApi")
@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    ServiceCompat.startForeground(this, NotificationHelper.PROGRESS_NOTIFICATION_ID,
            NotificationHelper.createProgressNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

    String[] questionTitles = intent.getStringArrayExtra(EXTRA_QUESTION_TITLES);
    String[] answers = intent.getStringArrayExtra(EXTRA_ANSWERS);
    long startDate = intent.getLongExtra(EXTRA_START_DATE, 0);
    AppExecutor.runInBackground(() -> generatePlan(questionTitles, answers, startDate));
    return START_NOT_STICKY;
}
```

1. `ServiceCompat.startForeground(...)` **pretvara servis u foreground servis** i prikazuje notifikaciju „Pravimo vaš plan…“. Ovo mora da se uradi **odmah**: ako foreground servis za nekoliko sekundi ne pozove `startForeground`, sistem sruši aplikaciju. Parametri su ID notifikacije, sama notifikacija (pravi je `NotificationHelper`, poglavlje 9) i tip posla (razmena podataka).
   - `@SuppressLint("InlinedApi")` gasi upozorenje alata: konstanta tipa servisa postoji tek od Androida 10, ali `ServiceCompat` na starijim verzijama tip sam ignoriše, pa je bezbedno.
2. Iz Intent-a vadimo podatke koje je poslao upitnik.
3. Sam posao predajemo **pozadinskoj niti**.
4. `START_NOT_STICKY` znači: ako sistem ipak ubije servis (npr. kritično malo memorije), nemoj ga sam ponovo pokretati.

```java
@Override
public IBinder onBind(Intent intent) {
    return null;
}
```
Servis može i da se „veže“ za aktivnost radi stalne komunikacije (*bound service*). Mi to ne koristimo, pa vraćamo `null`. Metoda mora da postoji jer je `Service` zahteva.

### 7.5 Posao na pozadinskoj niti

```java
private void generatePlan(String[] questionTitles, String[] answers, long startDate) {
    try {
        long tripId = generateAndSavePlan(questionTitles, answers, startDate);
        AppExecutor.runOnMainThread(() -> {
            pendingTripId = tripId;
            deliverPendingResult();
        });
    } catch (IOException e) {
        postError(R.string.error_network);
    } catch (JSONException e) {
        postError(R.string.error_response);
    } finally {
        stopSelf();
    }
}
```

- `try` pokušava ceo posao (7.6).
- Ako uspe, na glavnoj niti zapisujemo rezultat i pokušavamo da ga predamo slušaocu.
- `catch (IOException e)` hvata **mrežne greške**: nema interneta, server odgovori greškom (npr. preopterećen je), isteklo je vreme. Poruka je „Nije moguće povezati se sa serverom…“.
- `catch (JSONException e)` hvata slučaj kada je **odgovor stigao, ali nije ispravan JSON** u obliku koji očekujemo. Poruka je „Server je vratio neispravan odgovor.“.
- `finally { stopSelf(); }`: u svakom slučaju, posle posla servis **zaustavlja sam sebe**. Sistem ga uništi i notifikacija „Pravimo vaš plan…“ nestaje. Servis koji bi radio zauvek trošio bi bateriju.

```java
private long generateAndSavePlan(String[] questionTitles, String[] answers, long startDate)
        throws IOException, JSONException {
    GeminiService.PlanResult plan = new GeminiService().generatePlan(questionTitles, answers);
    Trip trip = new Trip(0, answers[Q_DESTINATION], Integer.parseInt(answers[Q_DAYS]),
            startDate, answers[Q_STYLE], answers[Q_BUDGET], plan.getSummary(),
            System.currentTimeMillis());
    long tripId = DatabaseHelper.getInstance(this).insertTripWithItems(trip, plan.getItems());
    NotificationHelper.showPlanReady(this, tripId, trip.getDestination());
    return tripId;
}
```

Ovo su **tri glavna koraka cele aplikacije**, u četiri reda:
1. **Gemini:** pošalji odgovore i dobij plan, odnosno kratak opis (`summary`) i listu stavki.
   > ➡️ **Poziva:** `GeminiService.generatePlan()` → poglavlje 8
2. Napravi objekat `Trip` od odgovora i opisa. ID je `0` jer pravi ID dodeljuje baza. `System.currentTimeMillis()` je trenutno vreme, kao `created_at`.
3. **Baza:** upiši putovanje i sve stavke u jednoj transakciji i dobij ID novog putovanja.
   > ➡️ **Poziva:** `DatabaseHelper.insertTripWithItems()` → 5.4
4. **Notifikacija:** prikaži „Vaš plan putovanja je spreman“.
   > ➡️ **Poziva:** `NotificationHelper.showPlanReady()` → poglavlje 9

`throws IOException, JSONException` znači da metoda ne hvata ove greške sama, nego ih prosleđuje pozivaocu (`generatePlan` iznad), koji ih hvata.

### 7.6 Predaja rezultata

```java
private void postError(int messageRes) {
    AppExecutor.runOnMainThread(() -> {
        pendingErrorRes = messageRes;
        deliverPendingResult();
    });
}

public static void setListener(Listener newListener) {
    listener = newListener;
    if (listener != null) {
        deliverPendingResult();
    }
}

private static void deliverPendingResult() {
    if (listener == null) {
        return;
    }
    if (pendingTripId != NO_TRIP) {
        long tripId = pendingTripId;
        pendingTripId = NO_TRIP;
        listener.onPlanReady(tripId);
    } else if (pendingErrorRes != NO_ERROR) {
        int messageRes = pendingErrorRes;
        pendingErrorRes = NO_ERROR;
        listener.onPlanFailed(messageRes);
    }
}
```

Zamisli **poštansko sanduče**:
- Kada je plan gotov (ili je stigla greška), servis **ubaci rezultat u sanduče** (`pendingTripId` ili `pendingErrorRes`) i pozove `deliverPendingResult`.
- `deliverPendingResult` proveri da li neko čeka (`listener`):
  - **Ako čeka**, uzme rezultat iz sanduka, isprazni sanduče i preda rezultat (`onPlanReady` ili `onPlanFailed` u upitniku, 6.12).
  - **Ako ne čeka** (korisnik je van aplikacije), rezultat ostaje u sanduku.
- Kad se upitnik ponovo pojavi na ekranu, u `onStart` pozove `setListener(this)`, a to odmah poziva `deliverPendingResult`. Ako u sanduku nešto čeka, upitnik to odmah dobije.

**Ceo tok kada korisnik izađe iz aplikacije:**
1. Korisnik dodirne „Generiši plan“, pa pritisne Home.
2. Upitnik: `onStop` → `setListener(null)`, i niko više ne sluša.
3. Servis radi dalje: Gemini → baza → notifikacija „Vaš plan putovanja je spreman“.
4. Rezultat ostaje u sanduku, jer niko ne sluša.
5. Korisnik dodirne notifikaciju, i otvara se plan (poglavlje 9). **Ili** se korisnik vrati u aplikaciju preko liste otvorenih aplikacija: upitnik → `onStart` → `setListener(this)` → rezultat iz sanduka → `onPlanReady` → plan se otvara.

### Proveri se

1. Zašto običan `ExecutorService` nije dovoljan kada korisnik izađe iz aplikacije?
2. Zašto foreground servis mora da prikazuje notifikaciju?
3. Zašto servis i dalje koristi `AppExecutor.runInBackground`?
4. Šta se dešava sa rezultatom ako korisnik nije u aplikaciji kada plan bude gotov?

<details><summary>Odgovori</summary>

1. Android aplikaciji u pozadini blokira internet i zamrzava je, a pri potpunom zatvaranju gasi proces. Nit staje pre nego što Gemini odgovori.
2. Tako korisnik zna da aplikacija radi nešto u pozadini. To je uslov koji sistem postavlja da bi takvu aplikaciju pustio da radi.
3. Zato što i servis radi na glavnoj niti, pa bi mrežni poziv u njemu bacio grešku ili zamrznuo aplikaciju.
4. Plan je već sačuvan u bazi i prikazana je notifikacija. ID ostaje u `pendingTripId` dok se upitnik ne vrati i ne pozove `setListener`.

</details>

---

## 8. Gemini API: GeminiService

📄 `GeminiService.java`

### 8.1 Osnovni pojmovi: API, HTTP, JSON

- **API** (Application Programming Interface) je način na koji jedan program koristi drugi. Gemini API je Google-ov servis kome naš program pošalje pitanje, a dobije odgovor veštačke inteligencije.
- **REST** znači da API koristimo preko običnih **HTTP** zahteva, kao što browser otvara stranice: na određenu adresu (URL) šaljemo zahtev i dobijamo odgovor.
- **POST** je vrsta HTTP zahteva kojom **šaljemo podatke** serveru (naše pitanje).
- **JSON** je tekstualni format za podatke, na primer `{"ime": "Rim", "dani": 3}`. Vitičaste zagrade `{}` su objekat (parovi ključ → vrednost), a uglaste `[]` su niz. I zahtev i odgovor su u JSON-u.
- **HTTP status kod** je broj kojim server javlja ishod: `200` je uspeh, `404` znači da ne postoji (npr. pogrešan model), `429` je previše zahteva, a `503` znači da je server preopterećen.

### 8.2 Konstante

```java
// Naziv modela; može se promeniti ako Google preimenuje besplatne modele.
private static final String MODEL = "gemini-3.5-flash";
private static final String ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";
private static final int CONNECT_TIMEOUT_MS = 15000;
private static final int READ_TIMEOUT_MS = 90000;
private static final String CODE_FENCE = "```";
```

- `MODEL` je koji Gemini model koristimo. Google povremeno **povlači stare modele** ili su neki modeli privremeno preopterećeni. Zato je ime modela **na jednom mestu**: ako model prestane da radi, menja se samo ovaj red. Upravo to se desilo tokom izrade: prvobitni model je ukinut za nove korisnike, a jedan noviji je bio stalno preopterećen. Prešli smo na `gemini-3.5-flash`, koji je radio pouzdano.
- `ENDPOINT` je adresa na koju šaljemo zahtev. U nju je ugrađeno ime modela.
- `CONNECT_TIMEOUT_MS = 15000`: koliko najviše čekamo da se uspostavi veza (15 s).
- `READ_TIMEOUT_MS = 90000`: koliko najviše čekamo odgovor (90 s), jer pravljenje plana traje.
- Ako se rokovi probiju, dobijamo `IOException`, to jest poruku o grešci u upitniku.

### 8.3 API ključ: odakle dolazi

Google mora da zna **ko** šalje zahtev, zato se uz zahtev šalje **API ključ**. Ključ je kao lozinka, pa **ne sme biti u kodu**: kod ide na GitHub, i svako bi mogao da ga vidi i troši.

Kako ključ stiže do koda:
1. Upišeš ga u `local.properties` (u korenu projekta):
   ```
   GEMINI_API_KEY=tvoj_ključ
   ```
   Ovaj fajl je u `.gitignore`, pa nikad ne ide na git.
2. `app/build.gradle.kts` pri svakom build-u pročita taj fajl:
   ```kotlin
   val localProperties = Properties()
   val localPropertiesFile = rootProject.file("local.properties")
   if (localPropertiesFile.exists()) {
       localPropertiesFile.inputStream().use { localProperties.load(it) }
   }
   ...
   buildConfigField("String", "GEMINI_API_KEY",
       "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
   ...
   buildFeatures { buildConfig = true }
   ```
   (Ovaj fajl je pisan u Kotlin skript jeziku, a ne u Javi. To je samo konfiguracija build-a, a sav kod aplikacije je u Javi.)
3. Gradle zatim **sam generiše** Java klasu `BuildConfig` sa konstantom `BuildConfig.GEMINI_API_KEY`.
4. `GeminiService` koristi tu konstantu (8.5).

Zato posle promene ključa treba uraditi **Sync** i **Run**, da bi se `BuildConfig` ponovo generisao.

### 8.4 Glavna metoda i pravljenje upita (prompt)

```java
public PlanResult generatePlan(String[] questionTitles, String[] answers)
        throws IOException, JSONException {
    String requestBody = buildRequestBody(buildPrompt(questionTitles, answers));
    String response = sendRequest(requestBody);
    return parseResponse(response);
}
```

Tri koraka: **napravi zahtev → pošalji ga → raščlani odgovor.**

```java
private String buildPrompt(String[] questionTitles, String[] answers) {
    StringBuilder prompt = new StringBuilder(PROMPT_INTRO);
    for (int i = 0; i < questionTitles.length; i++) {
        prompt.append("- ").append(questionTitles[i])
                .append(": ").append(answers[i]).append('\n');
    }
    return prompt.append(PROMPT_RULES).toString();
}
```

**Prompt** je tekst uputstva koje šaljemo modelu. Sastoji se od tri dela:
1. `PROMPT_INTRO`: „Ti si iskusan turistički vodič. Napravi plan putovanja po danima na osnovu odgovora korisnika. Odgovori korisnika:“
2. Odgovori, red po red:
   ```
   - Destinacija: Rim, Italija
   - Broj dana: 3
   - Datum polaska: 12.10.2026
   - Tip putovanja: Kombinovano
   ...
   ```
3. `PROMPT_RULES`, pravila odgovora: srpski jezik, latinica; 4 do 6 stavki po danu; prvi dan mora imati smeštaj (`SMESTAJ`); svaki dan bar jedna stavka `HRANA`; tačno određeni tipovi; vreme u formatu HH:mm; i **vrati isključivo JSON tačno ovog oblika**:
   ```json
   {"summary": "kratak opis putovanja u 2-3 rečenice",
    "items": [{"day": 1, "time": "09:00", "type": "ZNAMENITOST", "title": "...", "description": "..."}]}
   ```

`StringBuilder` je efikasan način spajanja mnogo delova teksta, bolji od ponovljenog `+` u petlji.

Prompt nije u `strings.xml` jer **nije tekst za korisnika**, nego uputstvo za model, i ne treba da se prevodi zajedno sa aplikacijom. To piše i u komentaru u kodu.

### 8.5 Telo zahteva i slanje

```java
private String buildRequestBody(String prompt) throws JSONException {
    JSONObject part = new JSONObject().put("text", prompt);
    JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
    JSONObject config = new JSONObject().put("responseMimeType", "application/json");
    return new JSONObject()
            .put("contents", new JSONArray().put(content))
            .put("generationConfig", config)
            .toString();
}
```

Gemini očekuje tačno određen oblik zahteva. Ovaj kod pravi:
```json
{
  "contents": [ { "parts": [ { "text": "...naš prompt..." } ] } ],
  "generationConfig": { "responseMimeType": "application/json" }
}
```
- `JSONObject` i `JSONArray` su klase iz paketa `org.json`, koji je **ugrađen u Android**, pa nam ne treba dodatna biblioteka.
- `"responseMimeType": "application/json"` traži od modela da odgovori **čistim JSON-om**, a ne običnim tekstom.

```java
private String sendRequest(String body) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
    try {
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);
        connection.setDoOutput(true);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + responseCode);
        }
        return readStream(connection.getInputStream());
    } finally {
        connection.disconnect();
    }
}
```

Red po red:
- `new URL(ENDPOINT).openConnection()` priprema vezu ka Google-ovom serveru. **`HttpURLConnection`** je Javina ugrađena klasa za HTTP, pa nam ne treba biblioteka poput Retrofit-a.
- `setRequestMethod("POST")`: šaljemo podatke.
- Postavljamo rokove čekanja (8.2).
- **Zaglavlja (headers)** su dodatne informacije uz zahtev:
  - `Content-Type: application/json; charset=utf-8`: „šaljem JSON, kodiran u UTF-8“ (zbog slova č, ć, š…).
  - `x-goog-api-key: ...`: naš API ključ iz `BuildConfig`-a.
- `setDoOutput(true)`: najavljujemo da ćemo slati telo zahteva.
- `getOutputStream()` + `write(...)`: šaljemo JSON tekst kao bajtove. `try (...)` automatski zatvara tok.
- `getResponseCode()`: **tek ovde** zahtev stvarno odlazi i čekamo odgovor. Ovaj red traje ~10–20 sekundi.
- Ako status nije `200` (OK), bacamo `IOException`, i servis prikazuje poruku o grešci.
- `readStream(...)` čita telo odgovora kao tekst. U pomoćnoj metodi `BufferedReader` čita red po red i spaja ih u `StringBuilder`.
- `finally { connection.disconnect(); }` zatvara vezu u svakom slučaju.

### 8.6 Raščlanjivanje (parsiranje) odgovora

Gemini vraća odgovor ovog oblika (skraćeno):
```json
{
  "candidates": [
    { "content": { "parts": [ { "text": "{\"summary\": \"...\", \"items\": [...]}" } ] } }
  ]
}
```
Naš plan je **JSON upakovan kao tekst** unutar polja `text`.

```java
private PlanResult parseResponse(String response) throws JSONException {
    String text = new JSONObject(response)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts").getJSONObject(0)
            .getString("text");
```
Spuštamo se kroz strukturu: niz `candidates`, prvi element `[0]`, objekat `content`, niz `parts`, prvi element `[0]`, polje `text`. Ako bilo koji deo nedostaje, `org.json` baca `JSONException`.

```java
    JSONObject plan = new JSONObject(stripCodeFences(text));
    JSONArray itemsJson = plan.getJSONArray("items");
    List<PlanItem> items = new ArrayList<>();
    for (int i = 0; i < itemsJson.length(); i++) {
        items.add(parseItem(itemsJson.getJSONObject(i)));
    }
    return new PlanResult(plan.getString("summary"), items);
}
```
- Tekst ponovo pretvaramo u JSON objekat, ovog puta u **naš** plan.
- Za svaku stavku u nizu `items` pravimo `PlanItem` objekat.
- Vraćamo `PlanResult`: kratak opis + lista stavki. `PlanResult` je mala klasa unutar `GeminiService`-a koja samo drži ta dva podatka zajedno, jer metoda u Javi može da vrati samo jednu vrednost.

```java
private PlanItem parseItem(JSONObject json) throws JSONException {
    return new PlanItem(0, json.getInt("day"), json.getString("time"),
            json.getString("type"), json.getString("title"),
            json.getString("description"), false, "");
}
```
Jedna stavka: ID `0` (pravi dodeljuje baza), dan, vreme, tip, naslov, opis, `false` (nije urađeno) i `""` (nema beleške).

```java
// Model ponekad obavije JSON u ```json ... ```, pa te oznake uklanjamo.
private String stripCodeFences(String text) {
    String result = text.trim();
    if (result.startsWith(CODE_FENCE)) {
        result = result.substring(result.indexOf('\n') + 1);
    }
    if (result.endsWith(CODE_FENCE)) {
        result = result.substring(0, result.length() - CODE_FENCE.length());
    }
    return result.trim();
}
```
Jezički modeli ponekad JSON „upakuju“ u Markdown oznake za kod, ovako:
````
```json
{ ... }
```
````
Tada to nije ispravan JSON. Ova metoda **odbrambeno** uklanja te oznake: ako tekst počinje sa ```` ``` ````, odseca prvi red, a ako se završava sa ```` ``` ````, odseca kraj.

### Proveri se

1. Gde je API ključ i zašto nije u kodu?
2. Šta znači `"responseMimeType": "application/json"`?
3. Kojim putem stižemo do teksta plana u odgovoru?
4. Zašto je ime modela konstanta na jednom mestu?
5. Kada `sendRequest` baca `IOException`?

<details><summary>Odgovori</summary>

1. U `local.properties`, koji ne ide na git. Gradle ga pri build-u prepisuje u `BuildConfig.GEMINI_API_KEY`. Da je u kodu, bio bi javan na GitHub-u.
2. Traži od modela da odgovori čistim JSON-om.
3. `candidates[0].content.parts[0].text`, a taj tekst je ponovo JSON sa `summary` i `items`.
4. Google povlači ili preimenuje modele, a neki budu preopterećeni. Promena se tada radi u jednom redu.
5. Kada nema veze, kada istekne rok čekanja ili kada server vrati status različit od 200 (npr. 404, 429, 503).

</details>

---

## 9. Notifikacije: NotificationHelper

📄 `NotificationHelper.java`

Aplikacija pravi dve notifikacije:
1. **„Pravimo vaš plan…“**: stoji dok radi servis (7.4).
2. **„Vaš plan putovanja je spreman“**: kad je plan sačuvan. Dodir na nju otvara plan.

### 9.1 Kanal

```java
private static final String CHANNEL_ID = "trip_plans";

private static void createChannel(Context context) {
    NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT);
    context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
}
```

- Od Androida 8 **svaka notifikacija mora pripadati kanalu**. Kanal je kategorija notifikacija koju korisnik može posebno da uključi ili isključi u podešavanjima telefona (naš se zove „Planovi putovanja“).
- `IMPORTANCE_DEFAULT`: notifikacija ima zvuk i ikonicu u statusnoj traci.
- `NotificationManager` je sistemski servis za notifikacije.
- Kanal pravimo pre svake notifikacije. Ako već postoji, sistem ništa ne menja, pa je ponovno pravljenje bezbedno.

### 9.2 Notifikacija dok traje rad

```java
public static final int PROGRESS_NOTIFICATION_ID = -1;

public static Notification createProgressNotification(Context context) {
    createChannel(context);
    return new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.loading_plan))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .build();
}
```
- `NotificationCompat.Builder` gradi notifikaciju (isti *builder* stil kao kod dijaloga).
- `setSmallIcon`: ikonica aviona u statusnoj traci (`res/drawable/ic_notification.xml`).
- `setProgress(0, 0, true)`: traka „neodređenog“ napretka, jer ne znamo koliko je ostalo.
- `setOngoing(true)`: korisnik ne može da je skloni prevlačenjem dok servis radi.
- `setSilent(true)`: bez zvuka, jer zvuk treba tek kad je plan gotov.
- ID je `-1`: ID-jevi putovanja su pozitivni, pa se ova notifikacija nikad ne poklopi sa notifikacijom nekog putovanja.

### 9.3 Notifikacija „Vaš plan putovanja je spreman“

```java
public static void showPlanReady(Context context, long tripId, String destination) {
    createChannel(context);
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
        return;
    }

    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(destination)
            .setContentIntent(createOpenTripIntent(context, tripId))
            .setAutoCancel(true);

    NotificationManagerCompat.from(context).notify((int) tripId, builder.build());
}
```

- Prvo proveravamo dozvolu. Ako je korisnik odbio notifikacije (2.6), samo izlazimo. Plan je svejedno sačuvan.
- Naslov je „Vaš plan putovanja je spreman“, a tekst ispod naslova je destinacija.
- `setContentIntent(...)`: **šta se dešava na dodir** (9.4).
- `setAutoCancel(true)`: notifikacija nestaje kad je korisnik dodirne.
- `notify((int) tripId, ...)` prikazuje notifikaciju. Prvi parametar je njen ID, i koristimo ID putovanja. Ako se naprave dva plana, biće dve odvojene notifikacije, a ne jedna koja zamenjuje drugu.

### 9.4 PendingIntent: šta se dešava na dodir

```java
private static PendingIntent createOpenTripIntent(Context context, long tripId) {
    Intent intent = new Intent(context, MainActivity.class)
            .putExtra(MainActivity.EXTRA_TRIP_ID, tripId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return PendingIntent.getActivity(context, (int) tripId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
}
```

- Notifikaciju dodiruje korisnik **kasnije**, možda kad naša aplikacija uopšte ne radi. Zato ne možemo da pozovemo `startActivity` sami. **`PendingIntent`** je Intent „za kasnije“, koji predajemo sistemu sa porukom: „kad korisnik dodirne notifikaciju, ti izvrši ovaj Intent u ime naše aplikacije“.
- Intent otvara `MainActivity` sa ID-jem putovanja:
  - `FLAG_ACTIVITY_NEW_TASK`: obavezno kada aktivnost otvara neko ko nije aktivnost (ovde sistem).
  - `CLEAR_TOP | SINGLE_TOP`: ako je `MainActivity` već otvorena, vrati nju i predaj joj Intent kroz `onNewIntent` (2.3).
- `getActivity(context, (int) tripId, ...)`: drugi parametar je „request code“. Pošto je različit za svako putovanje, svaka notifikacija ima svoj PendingIntent i otvara svoje putovanje.
- `FLAG_IMMUTABLE`: niko ne može da izmeni ovaj Intent. Od Androida 12 je obavezno navesti da li je PendingIntent promenljiv.

**Ceo tok na dodir notifikacije:** sistem otvara `MainActivity` → `onCreate` ili `onNewIntent` → `openTripFromIntent` → `openTrip(tripId)` → plan se prikazuje (poglavlje 10).

### Proveri se

1. Šta je kanal notifikacija i od koje verzije Androida je obavezan?
2. Zašto ne možemo samo da pozovemo `startActivity` kad korisnik dodirne notifikaciju?
3. Šta se dešava ako korisnik nije dao dozvolu za notifikacije?

<details><summary>Odgovori</summary>

1. Kategorija notifikacija koju korisnik može posebno da podesi. Obavezan je od Androida 8.
2. Zato što se dodir dešava kasnije, možda kad aplikacija ne radi. `PendingIntent` predaje sistemu Intent koji on izvršava u ime aplikacije.
3. `showPlanReady` izlazi bez prikaza notifikacije. Plan je svejedno sačuvan u bazi i vidi se u listi.

</details>

---

## 10. Ekran plana: TripDetailActivity, TripDetailFragment, PlanItemAdapter

Plan se otvara iz `MainActivity.openTrip()` (2.5): na tabletu kao fragment desno, a na telefonu kao nova aktivnost.

### 10.1 TripDetailActivity (samo telefon)

📄 `TripDetailActivity.java`

```java
public class TripDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Fragment dodajemo samo prvi put; posle rotacije ga sistem sam vraća.
        if (savedInstanceState == null) {
            long tripId = getIntent().getLongExtra(MainActivity.EXTRA_TRIP_ID, 0);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, TripDetailFragment.newInstance(tripId))
                    .commit();
        }
    }
}
```

Ova aktivnost je samo **„ram“** za fragment:
- Layout ima traku sa strelicom nazad i prazan `detail_container`.
- Strelica nazad (`setNavigationOnClickListener`) poziva `finish()`, koji zatvara ekran i vraća korisnika na listu.
- Iz Intent-a čita ID putovanja i dodaje `TripDetailFragment` za to putovanje.
- `if (savedInstanceState == null)`: posle rotacije Android **sam vraća** fragmente koji su bili prikazani. Kad bismo ga dodali ponovo, imali bismo dva fragmenta jedan preko drugog.

Zašto fragment, a ne sav kod u aktivnosti? Isti `TripDetailFragment` se na tabletu prikazuje **u `MainActivity`**. Kod plana je napisan jednom, a koristi se na dva mesta.

### 10.2 Pravljenje fragmenta: `newInstance`

📄 `TripDetailFragment.java`

```java
private static final String ARG_TRIP_ID = "trip_id";

public static TripDetailFragment newInstance(long tripId) {
    Bundle args = new Bundle();
    args.putLong(ARG_TRIP_ID, tripId);
    TripDetailFragment fragment = new TripDetailFragment();
    fragment.setArguments(args);
    return fragment;
}

public long getTripId() {
    return requireArguments().getLong(ARG_TRIP_ID);
}
```

Zašto ne konstruktor `new TripDetailFragment(tripId)`? Kada Android ponovo pravi fragment (posle rotacije), on poziva **prazan konstruktor**, pa bi parametar iz konstruktora bio izgubljen. **Argumenti** (`setArguments`) se, naprotiv, čuvaju i vraćaju automatski. Zato je `newInstance` sa argumentima standardan Android obrazac.

### 10.3 Izgled i učitavanje

```java
private TextView destinationText;
private TextView datesText;
private TextView summaryText;
private View calendarButton;
private PlanItemAdapter adapter;
private Trip trip;

@Override
public View onCreateView(...) {
    View view = inflater.inflate(R.layout.fragment_trip_detail, container, false);
    destinationText = view.findViewById(R.id.detail_destination);
    datesText = view.findViewById(R.id.detail_dates);
    summaryText = view.findViewById(R.id.detail_summary);
    calendarButton = view.findViewById(R.id.calendar_button);
    calendarButton.setOnClickListener(v -> onCalendarClick());

    adapter = new PlanItemAdapter(this);
    RecyclerView recycler = view.findViewById(R.id.plan_recycler);
    recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
    recycler.setAdapter(adapter);

    loadTrip();
    return view;
}
```

Layout `fragment_trip_detail.xml` ima:
- **zaglavlje**: destinacija (veliki tekst), datumi, kratak opis od Gemini-ja i dugme „Dodaj u kalendar“,
- **RecyclerView** sa stavkama plana.

Postupak je isti kao kod liste putovanja (3.2): pronađi View-ove, poveži dugme, napravi adapter i RecyclerView, pa učitaj podatke.

```java
private void loadTrip() {
    long tripId = getTripId();
    DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
    AppExecutor.runInBackground(() -> {
        Trip loadedTrip = db.getTrip(tripId);
        List<PlanItem> items = db.getPlanItems(tripId);
        AppExecutor.runOnMainThread(() -> showTrip(loadedTrip, items));
    });
}

private void showTrip(Trip loadedTrip, List<PlanItem> items) {
    if (!isAdded()) {
        return;
    }
    if (loadedTrip == null) {
        Toast.makeText(requireContext(), R.string.trip_not_found, Toast.LENGTH_SHORT).show();
        calendarButton.setVisibility(View.GONE);
        return;
    }
    trip = loadedTrip;
    destinationText.setText(trip.getDestination());
    datesText.setText(getString(R.string.date_range,
            Trip.formatDate(trip.getStartDate()), Trip.formatDate(trip.getEndDate())));
    summaryText.setText(trip.getSummary());
    adapter.setItems(items);
}
```

- Čitanje putovanja i njegovih stavki ide u pozadini, a prikaz na glavnoj niti (isti obrazac iz poglavlja 4).
- `loadedTrip == null` se dešava ako je putovanje u međuvremenu obrisano, npr. korisnik dodirne staru notifikaciju za obrisano putovanje. Tada prikazujemo **Toast**: kratku poruku pri dnu ekrana koja sama nestane.
- Pamtimo `trip` u polju jer nam treba za kalendar (poglavlje 11).

### 10.4 PlanItem model

📄 `PlanItem.java`

Kao `Trip`, ali za jednu stavku: `id`, `day`, `time`, `type`, `title`, `description`, `done`, `note`. Razlika je u tome što `done` i `note` **nisu `final`** i imaju **settere** (`setDone`, `setNote`), jer ih korisnik menja dok gleda plan.

### 10.5 PlanItemAdapter: jedna stavka plana

📄 `PlanItemAdapter.java`

Struktura je ista kao `TripAdapter` (3.5): interfejs za klikove, lista stavki, `onCreateViewHolder`, `onBindViewHolder`, `getItemCount`, ViewHolder. Ovde su zanimljivi detalji u `onBindViewHolder`:

```java
// Naslov dana prikazujemo samo kada se dan promeni u odnosu na prethodnu stavku.
boolean isNewDay = position == 0 || items.get(position - 1).getDay() != item.getDay();
holder.dayHeader.setVisibility(isNewDay ? View.VISIBLE : View.GONE);
holder.dayHeader.setText(context.getString(R.string.day_header, item.getDay()));
```

**Naslov „Dan 1“, „Dan 2“…** Svaka stavka u svom layout-u (`item_plan.xml`) ima TextView za naslov dana, iznad kartice. Prikazujemo ga samo ako je stavka **prva** (`position == 0`) ili ako je njen dan **različit od dana prethodne stavke**. Pošto su stavke poređane po danu (5.5), naslov se pojavljuje tačno na početku svakog dana. To je jednostavnije od RecyclerView-a sa više različitih vrsta redova.

```java
holder.icon.setText(iconForType(item.getType()));
holder.time.setText(item.getTime());
holder.title.setText(item.getTitle());
holder.description.setText(item.getDescription());
bindNote(holder, item);
bindDoneState(holder, item.isDone());
```

```java
private int iconForType(String type) {
    switch (type) {
        case "HRANA":
            return R.string.type_icon_food;          // 🍽️
        case "SMESTAJ":
            return R.string.type_icon_accommodation; // 🏨
        case "AKTIVNOST":
            return R.string.type_icon_activity;      // 🎯
        default:
            return R.string.type_icon_sight;         // 🏛️
    }
}
```
**Ikonica po tipu:** emoji (iz `strings.xml`) prema tipu stavke. `default` pokriva `ZNAMENITOST` i svaki neočekivani tip, pa uvek ima ikonicu.

```java
private void bindNote(PlanViewHolder holder, PlanItem item) {
    boolean hasNote = !item.getNote().isEmpty();
    holder.note.setVisibility(hasNote ? View.VISIBLE : View.GONE);
    holder.note.setText(holder.itemView.getContext()
            .getString(R.string.note_label, item.getNote()));
}
```
**Beleška** („Beleška: …“) se vidi samo ako postoji.

```java
private void bindDoneState(PlanViewHolder holder, boolean done) {
    holder.done.setChecked(done);
    int flags = holder.title.getPaintFlags();
    holder.title.setPaintFlags(done
            ? flags | Paint.STRIKE_THRU_TEXT_FLAG
            : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
    holder.card.setAlpha(done ? DONE_ALPHA : 1f);
}
```
**Urađena stavka:** štiklirano polje (CheckBox), **precrtan naslov** i **prigušena kartica**.
- „Paint flags“ su podešavanja načina crtanja teksta, a jedno od njih je precrtavanje.
- `flags | STRIKE` **uključuje** precrtavanje (bitovsko ILI).
- `flags & ~STRIKE` **isključuje** samo precrtavanje, a ostala podešavanja ostavlja (bitovsko I sa negacijom).
- `setAlpha(0.5f)` čini karticu poluprovidnom.

```java
holder.done.setOnClickListener(v -> {
    boolean done = holder.done.isChecked();
    item.setDone(done);
    bindDoneState(holder, done);
    listener.onDoneChanged(item, done);
});
holder.card.setOnClickListener(v ->
        listener.onItemClick(item, holder.getBindingAdapterPosition()));
```
- **Dodir na CheckBox:** odmah ažuriramo objekat i izgled, pa javljamo fragmentu da sačuva u bazu.
- **Dodir na karticu:** javljamo fragmentu, koji otvara dijalog za belešku. `getBindingAdapterPosition()` je trenutna pozicija stavke u listi.

### 10.6 Čuvanje „urađeno“ i beleške

📄 nazad u `TripDetailFragment.java`

```java
@Override
public void onDoneChanged(PlanItem item, boolean done) {
    DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
    AppExecutor.runInBackground(() -> db.setItemDone(item.getId(), done));
}
```
Izgled je već promenjen u adapteru, pa ovde samo upisujemo u bazu, u pozadini. Ništa ne treba vraćati na glavnu nit.

```java
@Override
public void onItemClick(PlanItem item, int position) {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_note, null);
    EditText noteInput = dialogView.findViewById(R.id.note_input);
    noteInput.setText(item.getNote());

    new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.note_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) ->
                    saveNote(item, position, noteInput.getText().toString().trim()))
            .setNegativeButton(R.string.cancel, null)
            .show();
}

private void saveNote(PlanItem item, int position, String note) {
    item.setNote(note);
    adapter.notifyItemChanged(position);
    DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
    AppExecutor.runInBackground(() -> db.updateItemNote(item.getId(), note));
}
```
- Dijalog dobija **sopstveni izgled**: `dialog_note.xml` sa poljem za tekst. `setView(dialogView)` ga ubacuje u dijalog.
- Polje je unapred popunjeno postojećom beleškom, da bi se mogla izmeniti.
- „Sačuvaj“:
  1. ažurira objekat,
  2. `notifyItemChanged(position)` ponovo iscrtava **samo tu jednu stavku**, pa se beleška odmah vidi,
  3. upisuje belešku u bazu u pozadini.

### Proveri se

1. Zašto plan postoji kao fragment, a ne samo kao aktivnost?
2. Zašto se fragment pravi preko `newInstance` sa argumentima, a ne preko konstruktora?
3. Kako adapter zna kada da prikaže naslov „Dan X“?
4. Šta se tačno dešava kad korisnik štiklira stavku?

<details><summary>Odgovori</summary>

1. Da bi isti kod mogao da se prikaže i u posebnoj aktivnosti (telefon) i u desnom delu `MainActivity` (tablet).
2. Android pri ponovnom pravljenju fragmenta (npr. posle rotacije) poziva prazan konstruktor. Argumenti se čuvaju i vraćaju automatski, a parametri konstruktora ne bi.
3. Prikazuje ga ako je stavka prva u listi ili ako se njen dan razlikuje od dana prethodne stavke.
4. Adapter odmah menja objekat i izgled (štiklirano, precrtano, prigušeno), pa javlja fragmentu, a fragment u pozadini upisuje `done = 1` u bazu.

</details>

---

## 11. Kalendar i Content Provider: CalendarHelper

### 11.1 Šta je Content Provider

Svaka Android aplikacija ima svoje privatne podatke. Druge aplikacije ne mogu direktno da im pristupe (kao što niko ne može da pročita našu bazu). Ali neke aplikacije **žele** da dele podatke: kontakti, kalendar, galerija…

**Content Provider** je standardni Android način da aplikacija **ponudi** svoje podatke drugima. Liči na malu bazu na koju se pristupa preko adrese (**URI**), na primer:
- `CalendarContract.Calendars.CONTENT_URI`: svi kalendari na uređaju,
- `CalendarContract.Events.CONTENT_URI`: svi događaji.

Mi tim podacima pristupamo preko **`ContentResolver`**-a, koji ima metode slične bazi: `query` (čitaj), `insert` (dodaj), `update` (izmeni), `delete` (obriši). Pristup je zaštićen dozvolama (`READ_CALENDAR`, `WRITE_CALENDAR`).

Važno: mi **ne otvaramo** aplikaciju Kalendar (to bi bio Intent), nego **direktno upisujemo** događaj u njene podatke.

### 11.2 Dugme i dozvole

📄 `TripDetailFragment.java`

```java
private static final String[] CALENDAR_PERMISSIONS = {
        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};

private final ActivityResultLauncher<String[]> calendarPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                result -> onCalendarPermissionResult());
```
- `registerForActivityResult(...)` je savremen način traženja dozvola. Unapred registrujemo „pokretač“ (launcher), a lambda se poziva kad korisnik odgovori na sistemski dijalog. Ovo mora da se uradi pri pravljenju fragmenta, zato je u deklaraciji polja.
- `RequestMultiplePermissions` traži više dozvola odjednom, ovde čitanje i pisanje kalendara.

```java
private void onCalendarClick() {
    if (trip == null) {
        return;
    }
    if (hasCalendarPermission()) {
        addToCalendar();
    } else {
        calendarPermissionLauncher.launch(CALENDAR_PERMISSIONS);
    }
}

private void onCalendarPermissionResult() {
    if (hasCalendarPermission()) {
        addToCalendar();
    } else {
        Toast.makeText(requireContext(), R.string.calendar_permission_denied,
                Toast.LENGTH_LONG).show();
    }
}

private boolean hasCalendarPermission() {
    for (String permission : CALENDAR_PERMISSIONS) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
    }
    return true;
}
```
Tok:
1. `trip == null` znači da putovanje još nije učitano, pa ne radimo ništa.
2. Ako dozvole već postoje, odmah dodajemo u kalendar.
3. Ako ne postoje, prikazujemo sistemski dijalog („Dozvoliti pristup kalendaru?“).
4. Kad korisnik odgovori: ako je dozvolio, dodajemo. Ako je odbio, prikazujemo Toast sa objašnjenjem.

```java
private void addToCalendar() {
    Context appContext = requireContext().getApplicationContext();
    Trip tripToAdd = trip;
    AppExecutor.runInBackground(() -> {
        boolean added = CalendarHelper.addTripToCalendar(appContext, tripToAdd);
        AppExecutor.runOnMainThread(() -> Toast.makeText(appContext,
                added ? R.string.calendar_added : R.string.calendar_not_found,
                Toast.LENGTH_LONG).show());
    });
}
```
Rad sa Content Provider-om je kao rad sa bazom, pa ide u pozadinu. Rezultat (`true`/`false`) određuje poruku: „Putovanje je dodato u kalendar.“ ili „Na uređaju nije pronađen nijedan kalendar…“.

### 11.3 Sam upis

📄 `CalendarHelper.java`

```java
public static boolean addTripToCalendar(Context context, Trip trip) {
    ContentResolver resolver = context.getContentResolver();
    long calendarId = findFirstCalendarId(resolver);
    if (calendarId == NO_CALENDAR) {
        return false;
    }
```
Događaj mora pripadati nekom kalendaru (npr. Google kalendaru korisnikovog naloga). Zato prvo tražimo ID prvog kalendara. Ako ga nema (emulator bez Google naloga), vraćamo `false`.

```java
private static long findFirstCalendarId(ContentResolver resolver) {
    String[] projection = {CalendarContract.Calendars._ID};
    try (Cursor cursor = resolver.query(CalendarContract.Calendars.CONTENT_URI,
            projection, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getLong(0);
        }
    }
    return NO_CALENDAR;
}
```
- `resolver.query(...)` je isti princip kao upit nad bazom (5.5), samo što se upit postavlja provider-u, preko URI-ja.
- `projection` su kolone koje tražimo, ovde samo `_ID`.
- Dobijamo isti `Cursor` kao kod baze. `moveToFirst()` vraća `false` ako nema nijednog kalendara.
- `cursor != null`: kod Content Provider-a `query` može da vrati `null` (npr. provider nije dostupan), za razliku od naše baze.

```java
    ContentValues values = new ContentValues();
    values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
    values.put(CalendarContract.Events.TITLE,
            context.getString(R.string.calendar_event_title, trip.getDestination()));
    values.put(CalendarContract.Events.DESCRIPTION, trip.getSummary());
    values.put(CalendarContract.Events.DTSTART, trip.getStartDate());
    values.put(CalendarContract.Events.DTEND,
            trip.getStartDate() + TimeUnit.DAYS.toMillis(trip.getDays()));
    values.put(CalendarContract.Events.ALL_DAY, 1);
    values.put(CalendarContract.Events.EVENT_TIMEZONE, "UTC");

    Uri eventUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values);
    return eventUri != null;
}
```
- Opet `ContentValues` (5.4), ali sada su ključevi kolone kalendara:
  - `CALENDAR_ID`: u koji kalendar ide događaj,
  - `TITLE`: „Putovanje: Rim, Italija“,
  - `DESCRIPTION`: kratak opis od Gemini-ja,
  - `DTSTART`: početak, to jest datum polaska,
  - `DTEND`: kraj. **Za celodnevne događaje kraj nije uključen**, pa je to prvi dan *posle* putovanja: početak + broj dana. Putovanje od 3 dana od 12.10. ima kraj 15.10. i u kalendaru zauzima 12, 13 i 14.
  - `ALL_DAY = 1`: celodnevni događaj, bez sata,
  - `EVENT_TIMEZONE = "UTC"`: celodnevni događaji moraju biti u UTC-u, a naši datumi već jesu (3.1).
- **`resolver.insert(Events.CONTENT_URI, values)` je ključni red celog zadatka o Content Provider-u.** Vraća URI novog događaja, ili `null` ako upis nije uspeo.

### Proveri se

1. Šta je Content Provider i preko čega mu pristupamo?
2. Zašto prvo tražimo ID kalendara?
3. Zašto je `DTEND` jedan dan posle poslednjeg dana putovanja?
4. Koja je razlika između našeg rešenja i otvaranja aplikacije Kalendar preko Intent-a?

<details><summary>Odgovori</summary>

1. Standardni Android mehanizam kojim aplikacija deli podatke sa drugima preko URI adresa. Pristupamo preko `ContentResolver`-a (`query`, `insert`, `update`, `delete`), uz dozvole.
2. Svaki događaj mora pripadati nekom kalendaru (nalogu) na uređaju.
3. Kod celodnevnih događaja kraj nije uključen, pa je kraj prvi dan posle putovanja.
4. Mi direktno upisujemo podatke preko `ContentResolver.insert` (pravi Content Provider). Intent bi samo otvorio drugu aplikaciju i ostavio korisniku da sam sačuva događaj.

</details>

---

## 12. Resursi, dizajn i build sistem

### 12.1 Tekstovi: `res/values/strings.xml`

Sav tekst koji korisnik vidi je u `strings.xml`, nikad direktno u kodu ili layout-u:
```xml
<string name="new_trip">Novo putovanje</string>
<string name="question_progress">Pitanje %1$d od %2$d</string>
<plurals name="days_count">
    <item quantity="one">%d dan</item>
    <item quantity="few">%d dana</item>
    <item quantity="other">%d dana</item>
</plurals>
```
- U kodu: `getString(R.string.new_trip)`. U XML-u: `@string/new_trip`.
- `%1$s` / `%1$d` su mesta za parametre: tekst (`s`) ili ceo broj (`d`). Broj pre `$` je redni broj parametra.
- `plurals` daje pravilne oblike reči prema broju (3.5).
- Prednosti: svi tekstovi su na jednom mestu, a aplikacija se lako prevodi (dodaje se npr. `values-en/strings.xml`).

### 12.2 Tema i boje

- `res/values/colors.xml`: boje, jedan akcenat (tamno tirkizna `#00696B`) i neutralne pozadine.
- `res/values/themes.xml`: tema `Theme.TravelApp` nasleđuje `Theme.Material3.Light.NoActionBar` (Google-ov **Material Design 3**, svetla varijanta, bez stare trake na vrhu, jer koristimo `MaterialToolbar`). U temi samo menjamo boje, a sve komponente (dugmad, kartice, dijalozi) ih same preuzimaju.
- `res/values/dimens.xml`: razmaci (8dp, 16dp, 24dp), da bi bili isti svuda.
- `res/color/choice_stroke.xml`: boja ivice kartice zavisi od stanja (izabrana / nije izabrana).
- `res/drawable/`: ikonice kao **vektorske** slike (XML). Oštre su na svakoj veličini ekrana.

### 12.3 Build sistem: Gradle

📄 `app/build.gradle.kts`

```kotlin
android {
    namespace = "com.example.travelapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.travelapp"
        minSdk = 26
        targetSdk = 36
        ...
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
```
- **Gradle** je alat koji prevodi kod i pakuje ga u instalacioni fajl (**APK**).
- `applicationId` je jedinstveno ime aplikacije na uređaju i u prodavnici.
- `minSdk = 26`: aplikacija radi od Androida 8.0 naviše. Zato kanal notifikacija pravimo bez provere verzije (kanali postoje od Androida 8).
- `targetSdk = 36`: aplikacija je pisana i testirana za Android 16 i poštuje njegova pravila (npr. prikaz od ivice do ivice).
- `compileSdk = 36`: sa kojom verzijom Android biblioteka se kod prevodi.
- **Zavisnosti (biblioteke)**, samo tri:
  - `appcompat`: `AppCompatActivity` i podrška za starije verzije Androida,
  - `material`: Material komponente (dugmad, kartice, dijalozi, kalendar, trake napretka),
  - `recyclerview`: liste.
- Namerno **nismo** koristili Retrofit/OkHttp (mreža), Gson (JSON) ni Room (baza). Sve to je urađeno ugrađenim Android/Java klasama (`HttpURLConnection`, `org.json`, `SQLiteOpenHelper`), da bi se jasno videlo kako stvari rade „ispod haube“.

### Proveri se

1. Zašto tekstovi nisu napisani direktno u kodu?
2. Šta znači `minSdk = 26`?
3. Zašto nismo koristili Retrofit i Room?

<details><summary>Odgovori</summary>

1. Da budu na jednom mestu, da se aplikacija lako prevodi i da se izbegnu greške pri menjanju teksta.
2. Aplikacija može da se instalira na Android 8.0 (API 26) i novije verzije.
3. Zadatak traži da se vide osnovni mehanizmi: ručni HTTP poziv i SQL. Uz to je projekat jednostavniji i ima manje zavisnosti.

</details>

---

## 13. Kako pokrenuti aplikaciju i šta pokazati na odbrani

### 13.1 Pokretanje

1. Otvori Android Studio → **File → Open** → izaberi folder `TravelApp`. Sačekaj da se završi Gradle sinhronizacija (traka napretka dole).
2. Proveri API ključ: u levom panelu izaberi prikaz **Project** (padajući meni gore levo), otvori `local.properties` i proveri da red `GEMINI_API_KEY=...` sadrži ključ. Ako si ga menjala: **File → Sync Project with Gradle Files**.
3. Pokreni emulator: **Device Manager** (ikonica telefona desno) → ▶ pored uređaja.
4. U gornjoj traci proveri da piše **app** i ime emulatora, pa pritisni zeleno **▶ Run**.
5. Posle svake promene koda ponovo pritisni **Run**. Samo ponovno otvaranje aplikacije na emulatoru **ne** učitava novi kod.

**Ako generisanje ne uspe** („Nije moguće povezati se sa serverom…“):
- Sačekaj minut i pritisni **„Pokušaj ponovo“**. Besplatni modeli su ponekad privremeno zauzeti.
- Ako stalno ne radi, model je možda ukinut. Ime modela se menja u jednom redu, `MODEL` na vrhu `GeminiService.java` (8.2).

### 13.2 Scenario za odbranu (redom, ~5 minuta)

| # | Šta radiš | Šta pokazuje (pojam) |
|---|---|---|
| 1 | Otvori aplikaciju sa praznom listom | Poruka „Još nemate sačuvanih putovanja“ |
| 2 | „Novo putovanje“: upiši destinaciju, pa za broj dana upiši 20 | „Dalje“ ostaje sivo (provera 1–14) |
| 3 | Ispravi na 3, otvori izbor datuma | Prošli datumi su sivi |
| 4 | Na pitanju sa karticama izaberi opciju, pa **rotiraj emulator** | Ostaješ na istom pitanju sa istim odgovorima (`onSaveInstanceState`) |
| 5 | Završi upitnik → „Generiši plan“ → odmah pritisni **Home** | Notifikacija „Pravimo vaš plan…“ (**foreground servis**, **niti**) |
| 6 | Sačekaj notifikaciju „Vaš plan putovanja je spreman“ i dodirni je | **Notifikacije** + PendingIntent; otvara se plan (**internet servis**) |
| 7 | U planu štikliraj stavku, dodirni karticu i dodaj belešku | Precrtavanje i beleška |
| 8 | Zatvori aplikaciju potpuno i otvori je ponovo | Stavka je i dalje štiklirana, beleška je tu (**SQLite**) |
| 9 | „Dodaj u kalendar“ → dozvoli pristup | **Content Provider**; poruka o uspehu ili „nije pronađen kalendar“ |
| 10 | U listi dugo pritisni putovanje → „Obriši“ | Dijalog za potvrdu, brisanje, CASCADE |
| 11 | Pokreni **tablet emulator** | Lista levo, plan desno (**dva layout-a**, fragmenti) |

**Napomene:**
- Za korak 9: emulator bez Google naloga nema kalendar, pa aplikacija prikaže poruku da kalendar nije pronađen. To je ispravno ponašanje. Za pravi upis treba se na emulatoru prijaviti Google nalogom (Settings → Passwords & accounts) ili koristiti pravi telefon.
- Za korak 11: u Device Manager-u napravi novi uređaj (**+** → Create Virtual Device → kategorija **Tablet**, npr. „Pixel Tablet“) i pokreni aplikaciju na njemu.
- Pre odbrane prođi ceo scenario bar jednom, da emulatori budu spremni i da plan bude već generisan, za slučaj da je Gemini u tom trenutku spor.

### 13.3 Kratka mapa: koji pojam je u kom fajlu

| Pojam sa ispita | Fajl(ovi) | Poglavlje |
|---|---|---|
| Dva layout-a (telefon/tablet), fragmenti | `layout/activity_main.xml`, `layout-sw600dp/activity_main.xml`, `MainActivity.isTablet()`, `openTrip()` | 2 |
| Internet servis | `GeminiService.java` | 8 |
| Lokalna baza | `DatabaseHelper.java` | 5 |
| Niti | `AppExecutor.java` (+ `PlanGenerationService`) | 4, 7 |
| Notifikacije | `NotificationHelper.java`, `MainActivity.requestNotificationPermission()` | 9, 2 |
| Content Provider | `CalendarHelper.java`, `TripDetailFragment.onCalendarClick()` | 11 |

Za sam dan odbrane pogledaj i `ODBRANA.md`: kratko, sa tipičnim pitanjima profesora.
