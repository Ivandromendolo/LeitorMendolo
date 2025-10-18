Leitor de Música MVP (LeitorMendolo)
Um leitor de música nativo para Android, desenvolvido em Java, focado numa arquitetura limpa e num conjunto robusto de funcionalidades. Este projeto implementa o padrão Model-View-Presenter (MVP) para gerir a lógica da aplicação, garantindo que a interface do utilizador, a lógica de negócio e a gestão de dados estão devidamente desacopladas.

A aplicação permite a reprodução de música local, gestão de playlists, e inclui uma funcionalidade avançada de reconhecimento de música através da API ACRCloud.

Funcionalidades Principais
Reprodução de Áudio Completa: Controlo total sobre a reprodução com funções de Play, Pause, Próxima e Anterior.

Serviço em Background: A música continua a tocar de forma estável mesmo quando a aplicação está em segundo plano ou o ecrã está desligado, graças ao uso de um Service de primeiro plano (ForegroundService).

Notificação de Mídia: Controlos de reprodução integrados no painel de notificações, sincronizados com a MediaSessionCompat do Android. Inclui barra de progresso em tempo real.

Gestão de Áudio Focus: A aplicação pausa automaticamente a reprodução ao receber chamadas ou outras interrupções de áudio e pode resumir após a interrupção.

Gestão de Playlists:

Base de Dados Room: As playlists e as relações entre músicas e playlists são guardadas localmente numa base de dados SQLite gerida pelo Room.

Criar Playlists: O utilizador pode criar novas playlists através de um diálogo.

Adicionar Músicas: O utilizador pode adicionar qualquer música da lista principal a uma ou mais playlists através de um clique longo.

Apagar Playlists: O utilizador pode apagar playlists existentes através de um clique longo (com diálogo de confirmação).

Ver Playlists: Abertura de uma tela de detalhes (PlaylistDetailActivity) para ver as músicas contidas numa playlist específica.

Controlo de Fila: Funcionalidades de Shuffle (modo aleatório) e Repeat (repetir música).

Interface Dinâmica:

SeekBar atualizada em tempo real (via Handler) que permite ao utilizador arrastar para mudar a posição da música.

Listas de músicas e playlists que se atualizam automaticamente após qualquer alteração (criação, remoção).

Busca Rápida: SearchView para filtrar a lista principal de músicas por título ou artista.

Reconhecimento de Música: Integração com a API ACRCloud para identificar músicas que estejam a tocar no ambiente, acedido através do menu da aplicação.

Arquitetura do Projeto (MVP)
O projeto segue estritamente o padrão Model-View-Presenter para uma clara separação de responsabilidades.

1. Model (Camada de Dados)
Responsável por gerir os dados da aplicação e a sua persistência.

Entidades (Room): Song.java, Playlist.java, PlaylistSongCrossRef.java.

DAOs (Data Access Objects): SongDao.java, PlaylistDao.java.

Base de Dados: AppDatabase.java (classe abstrata que define a base de dados Room).

2. View (Camada de Interface)
Responsável apenas por exibir a UI e capturar as interações do utilizador.

Activities: MainActivity.java (tela principal) e PlaylistDetailActivity.java (tela de detalhes da playlist).

Adapters: SongAdapter.java e PlaylistAdapter.java (para popular os RecyclerViews).

Layouts XML: activity_main.xml, activity_playlist_detail.xml, song_item_layout.xml, playlist_item_layout.xml.

3. Presenter (Camada de Lógica)
O "cérebro" da aplicação. Faz a ponte entre o Model e a View, contendo toda a lógica de negócio.

Contrato: MusicPlayerContract.java define a interface de comunicação entre a View e o Presenter.

Implementação: MusicPlayerPresenter.java

Gere os eventos de clique da UI (ex: onPlayPauseClicked()).

Comunica com o MusicService para controlar a reprodução.

Executa todas as operações de base de dados (loadPlaylists, createPlaylist, addSongToPlaylist) numa thread de fundo (usando ExecutorService).

Utiliza um Handler da Main Thread para enviar os resultados da thread de fundo de volta para a View (ex: view.displayPlaylists(...)).

Tecnologias e Bibliotecas Utilizadas
Linguagem: Java

Arquitetura: MVP (Model-View-Presenter)

Base de Dados: AndroidX Room

Reprodução de Áudio: MediaPlayer gerido dentro de um Service

Componentes Android: ForegroundService, BroadcastReceiver, NotificationCompat, MediaSessionCompat

UI: RecyclerView, Material Design (FloatingActionButton), AlertDialog, Toolbar, SearchView

Threading: Handler (Main Thread) e ExecutorService (Background Threads)

API Externa: ACRCloud Universal SDK (para reconhecimento de áudio)

Configuração e Execução
Pré-requisitos
Android Studio

Um dispositivo Android ou Emulador (API 24+)

Passos de Configuração
Clonar o Repositório:

Bash

git clone [URL_DO_SEU_REPOSITÓRIO_GIT]
Abrir no Android Studio:

Abra o Android Studio e selecione "Open an existing project".

Navegue até à pasta do projeto e selecione-a.

Aguarde o Gradle sincronizar as dependências.

Configurar a API ACRCloud (Obrigatório): Esta aplicação não funcionará corretamente sem as credenciais da API de reconhecimento de música.

Crie uma conta gratuita no site oficial do ACRCloud.

No seu painel de controlo, crie um novo projeto do tipo "Audio Recognition".

Copie os seus Host, Access Key, e Access Secret.

Abra o ficheiro app/src/main/java/com/intro/leitormendolo/view/MainActivity.java.

Localize o método initAcrCloud() e cole as suas credenciais:

Java

private void initAcrCloud() {
    ACRCloudConfig config = new ACRCloudConfig();
    config.acrcloudListener = this;
    config.context = this;
    config.host = "O_SEU_HOST_AQUI"; // <-- Substitua
    config.accessKey = "A_SUA_ACCESS_KEY_AQUI"; // <-- Substitua
    config.accessSecret = "O_SEU_ACCESS_SECRET_AQUI"; // <-- Substitua
    // ... (resto do método)
}
Adicionar Ficheiros de Mídia (Opcional): A aplicação está configurada para carregar 3 músicas de exemplo (faded, anitta, adele) a partir dos recursos raw.

Adicione os seus ficheiros .mp3 ou .wav à pasta app/src/main/res/raw/.

Atualize os nomes dos ficheiros no método loadMusic() em MusicPlayerPresenter.java e no método getSongResource() em MusicService.java.

Adicione as imagens de capa correspondentes (ex: faded_cover.png) à pasta app/src/main/res/drawable/.

Compilar e Executar:

Clique no botão "Run" (ícone de play verde) no Android Studio.

Estrutura de Ficheiros
com.intro.leitormendolo
│
├── adapter/              # Adapters para os RecyclerViews
│   ├── PlaylistAdapter.java
│   └── SongAdapter.java
│
├── dao/                  # Data Access Objects (para o Room)
│   ├── PlaylistDao.java
│   └── SongDao.java
│
├── model/                # Classes de dados (Entidades do Room)
│   ├── Playlist.java
│   ├── PlaylistSongCrossRef.java
│   ├── PlaylistWithSongs.java
│   └── Song.java
│
├── presenter/            # Lógica de negócio
│   └── MusicPlayerPresenter.java
│
├── service/              # Serviço de reprodução de música
│   └── MusicService.java
│
├── view/                 # Activities (Telas)
│   ├── MainActivity.java
│   └── PlaylistDetailActivity.java
│
├── AppDatabase.java      # Definição da base de dados Room
└── MusicPlayerContract.java # Contrato da arquitetura MVP
