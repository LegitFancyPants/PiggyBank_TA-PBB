# PiggyBank

Aplikasi mobile pencatat keuangan pribadi berbasis Android untuk mengelola arus kas harian (pemasukan & pengeluaran), memantau target tabungan (*celengan*), menganalisis kategori lewat grafik, memantau anggaran bulanan, serta mengekspor laporan ke format Excel (`.xlsx`).

Aplikasi dirancang dengan mekanisme **offline-first** (tetap bisa dipakai dan menyimpan data saat tanpa internet lalu otomatis tersinkronisasi saat online) serta **isolasi data per perangkat** menggunakan `X-Device-Id`.

---

## Panduan Penggunaan Aplikasi

### Navigasi Utama (Bottom Bar)

Di bagian bawah layar utama terdapat 4 tab dan 1 tombol aksi utama di tengah:

1. **Kas** — Halaman utama untuk mencatat dan melihat riwayat transaksi harian per bulan.
2. **Celengan** — Halaman manajemen target tabungan dan pencatatan setoran.
3. **Tombol `+` (Tengah)** — Pintasan cepat untuk menambah data baru:
   - Saat berada di tab **Kas**, **Grafik**, atau **Profil**, menekan **`+`** membuka form **Tambah Transaksi**.
   - Saat berada di tab **Celengan**, menekan **`+`** membuka form **Target Tabungan Baru**.
4. **Grafik** — Visualisasi porsi pengeluaran & pemasukan per kategori.
5. **Profil** — Pengaturan nama profil, ringkasan Total Aset, **Laporan**, **Export Excel**, dan **Tentang Aplikasi**.

---

### 1. Halaman Kas (Arus Kas Harian)

- **Memilih Bulan & Tahun:**
  - Ketuk label bulan di pojok kanan atas (misal `Nov 2026`) untuk membuka dialog kalender, pilih bulan (`Jan`–`Des`) serta tahun yang bisa di-*scroll*, lalu ketuk **Konfirmasi**.
  - Kamu juga bisa berpindah bulan dengan gesture **tarik (*swipe*)**: tarik ke bawah saat sudah mentok paling atas untuk maju ke bulan berikutnya, atau tarik ke atas saat mentok paling bawah untuk mundur ke bulan sebelumnya (tetap berfungsi walaupun bulan kosong).
- **Ringkasan Saldo (Hero Card):**
  - Menampilkan **Saldo Bersih Saat Ini** beserta perbandingan **Pemasukan** dan **Pengeluaran** pada bulan aktif.
- **Filter, Pencarian, & Pengurutan:**
  - Gunakan chip **Semua**, **Pemasukan**, atau **Pengeluaran** untuk menyaring tipe transaksi.
  - Ketuk kolom **Cari transaksi...** untuk mencari berdasarkan judul maupun kategori (ketuk ikon **`X`** atau **Hapus Pencarian** untuk mereset).
  - Ketuk ikon pengurutan di sebelah kanan kolom pencarian untuk mengurutkan berdasarkan: *Dari Terbaru*, *Dari Terlama*, *Sesuai Abjad (A–Z)*, *Nominal Terbesar*, atau *Nominal Terkecil*.
- **Menambah, Mengubah, & Menghapus Transaksi:**
  1. Ketuk tombol **`+`** di navbar tengah.
  2. Pilih tipe (**Pengeluaran** atau **Pemasukan**). Opsi kategori akan menyesuaikan otomatis dengan tipe yang dipilih.
  3. Isi **Judul Transaksi**, **Nominal (Rp)**, **Kategori**, **Tanggal** (ketik format `YYYY-MM-DD` atau pilih lewat ikon kalender), dan **Catatan** (opsional), lalu ketuk **Simpan Transaksi**.
  4. Untuk melihat detail, mengedit, atau menghapus, ketuk salah satu transaksi di daftar **Riwayat Transaksi** → pilih **Edit** atau **Hapus**.

---

### 2. Halaman Celengan (Target Tabungan)

- **Memantau Target:**
  - Setiap kartu target menampilkan persentase progress, progress bar, nominal terkumpul terhadap target, serta penanda **Lewat tenggat** jika target belum terpenuhi melewati batas waktunya.
  - Gunakan filter status (**Semua**, **Belum Tercapai**, **Tercapai**), pencarian **Cari nama target...**, dan menu urut (*Sesuai Abjad (A–Z)*, *Dari Terlama*, *Dari Terbaru*, *Target Terbesar*, *Target Terkecil*).
- **Membuat Target Baru:**
  1. Buka tab **Celengan**, lalu ketuk tombol **`+`**.
  2. Isi **Nama Target Tabungan**, **Nominal Target (Rp)**, **Saldo Awal Terkumpul (Rp)** (isi `0` jika mulai dari awal), **Tenggat Waktu** (opsional), dan **Catatan / Motivasi** (opsional).
  3. Ketuk **Simpan Target**.
- **Menambah Setoran, Mengedit, & Menghapus:**
  1. Ketuk kartu target untuk membuka halaman **Detail Target**.
  2. Ketuk tombol **Tambah Setoran** di bagian bawah → masukkan nominal setoran (atau ketuk **Isi Sisa** untuk langsung mengisi seluruh sisa kekurangan) → ketuk **Simpan Setoran**.
  3. Jika tabungan sudah mencapai `100%`, status tombol otomatis berubah menjadi **Target Sudah Tercapai**.
  4. Gunakan ikon **Edit** (pensil) atau **Hapus** (tempat sampah) di bilah atas halaman Detail Target untuk mengubah atau menghapus celengan.

---

### 3. Halaman Grafik

- Gunakan switch di bagian atas untuk berpindah antara grafik **Pengeluaran** dan **Pemasukan**.
- Pilih bulan melalui strip tab horizontal atau ketuk ikon kalender di pojok kanan atas (bulan aktif tersinkron dengan halaman **Kas**).
- Halaman menampilkan **Donut Chart** beserta total nominal di bagian tengah, legenda ringkas, dan daftar rincian setiap kategori lengkap dengan nominal, persentase, serta warna ikon kategori yang konsisten.

---

### 4. Halaman Profil, Laporan, & Export Excel

- **Ubah Nama Profil & Total Aset:**
  - Ketuk kartu nama di bagian atas halaman **Profil** untuk mengubah nama panggilan (tersimpan lokal di perangkat).
  - **Total Aset** dihitung otomatis dari gabungan saldo kas seluruh waktu ditambah total saldo terkumpul di semua target celengan.
- **Laporan (`Profil → Laporan`):**
  - **Statistik Bulanan:** Ketuk kartu ringkasan bulan berjalan untuk melihat tabel rekap **Pengeluaran**, **Pemasukan**, dan **Saldo** per bulan, lengkap dengan filter **Semua** atau tahun tertentu di pojok kanan atas.
  - **Anggaran Bulanan:** Nilai default awal adalah `0` (*Belum diatur*). Ketuk kartu anggaran untuk membuka halaman **Anggaran**, pilih bulan lewat pemilih bulan kanan atas, lalu ketuk ikon pensil atau **Pengaturan Anggaran** untuk menentukan batas pengeluaran bulanan (isi `0` atau kosongkan untuk menonaktifkan). Indikator lingkaran menampilkan persentase pemakaian serta peringatan merah jika **Melebihi** batas.
- **Export Excel (`Profil → Export Excel`):**
  1. Pilih **Periode Transaksi** yang ingin diekspor: **Semua Data**, **Bulan** tertentu, atau **Tahun** tertentu.
  2. Centang **Konten Sheet Excel** yang ingin disertakan (minimal 1 sheet):
     - **Ringkasan Keuangan** — Total aset, pemasukan, pengeluaran, saldo bersih periode, status anggaran bulanan, ringkasan celengan, dan rekap per kategori.
     - **Riwayat Transaksi** — Daftar transaksi lengkap sesuai periode terpilih beserta rekap total di bawah tabel.
     - **Target Celengan** — Daftar seluruh target tabungan beserta nominal, progress, status, tenggat waktu, dan catatan.
  3. Pilih aksi output:
     - **Simpan ke Perangkat** untuk menyimpan file `.xlsx` ke penyimpanan HP (seperti folder *Downloads*).
     - **Bagikan File (.xlsx)** untuk mengirim file langsung lewat WhatsApp, Email, Google Drive, atau aplikasi lain.
- **Tentang Aplikasi (`Profil → Tentang Aplikasi`):** Menampilkan informasi versi aplikasi dan identitas pengembang.

---

### 5. Mode Offline & Pemisahan Data per Perangkat

- **Penyimpanan Offline & Sinkronisasi Otomatis:**
  - Jika perangkat tidak terhubung ke internet, seluruh catatan transaksi baru, edit, hapus, maupun setoran celengan tetap disimpan secara persisten di penyimpanan lokal dan langsung memperbarui tampilan aplikasi.
  - Begitu koneksi internet kembali tersedia atau aplikasi dibuka ulang dalam kondisi online, seluruh antrean perubahan lokal otomatis dikirim ke server dan disinkronkan.
- **Data Terpisah Antar Perangkat:**
  - Setiap instalasi aplikasi membuat ID perangkat unik (`X-Device-Id`), sehingga data di satu HP tidak memengaruhi HP lainnya.
  - *Catatan:* Menghapus data aplikasi (*Clear Storage*) atau mencopot pemasangan (*Uninstall*) akan membuat ID perangkat baru.

---

## Tech Stack Singkat

- **Bahasa & UI:** Kotlin `2.2.10`, Jetpack Compose (Material 3, Navigation Compose `2.8.9`, Material Icons Extended).
- **Arsitektur & Async:** MVVM (`AndroidViewModel`, `StateFlow`), Kotlin Coroutines.
- **Networking & Penyimpanan Lokal:** Retrofit `2.11.0`, OkHttp Interceptor (`X-Device-Id`), Gson, `SharedPreferences` (cache & antrean sync offline-first serta preferensi profil), `ConnectivityManager`, `FileProvider`, Storage Access Framework (SAF).
- **Export Excel:** Generator OOXML `.xlsx` multi-sheet *pure Kotlin* menggunakan `java.util.zip.ZipOutputStream` (tanpa dependensi eksternal berat).
- **Backend API:** Express.js REST API (`piggybank-api`) + Supabase PostgreSQL.
- **Build & Testing:** Android Gradle Plugin `9.3.2` (`minSdk 24`, `targetSdk 37`), JUnit 4.

---

## Menjalankan Proyek & Pengujian Lokal

1. Buka folder `PiggyBank` di **Android Studio** dan biarkan Gradle sync selesai, lalu jalankan aplikasi ke emulator atau perangkat Android (`minSdk 24`).
2. Untuk menjalankan seluruh unit test lewat terminal dari dalam folder `PiggyBank`:
   ```powershell
   .\gradlew.bat testDebugUnitTest --offline
   ```
