.. _ref-userguide_ja:
.. role:: alert

ユーザガイド
============

はじめに
--------

GroovyServは、Groovy処理系をサーバとして動作させることでgroovyコマンドの起動を見た目上高速化するものです。
groovyclientコマンドはTCP/IP 通信を使ってすでに起動しているGroovyランタイムと通信し、結果を出力します。
もし、Emacsのgnuserv(gnudoit)やemacsserver/emacsclientをご存知であれば理解が早いでしょう。

Groovyスクリプトを開発する場合、起動速度がとても重要です。
Groovyは動的言語であるため、コンパイル時にあらかじめチェックできないエラーについて、実行して初めて判明する場合が多いからです。
そのため、実行を繰り返しながら開発をしていくことになります。
たとえその起動が1..2秒しかかからなくても、体感としてはとても大きく感じられるのではないでしょうか。

GroovyServを使うことで、Groovyコマンドの起動時間を短縮し、さくさくと開発を進めていくことができます。
以下は、Windows XP Core(TM) 2 Duo 2.53GHzのマシンにおけるGroovy 1.8.0の起動時間の参考値です(5回測定した平均値)。

    ==================  ===========
    コマンド            結果(sec)
    ==================  ===========
    Groovy              1.1058
    GroovyServ          0.0412
    ==================  ===========

動作環境
--------

動作確認を行ったOSは以下のとおりです。

  - Mac OS X 10.8 (Intel Mac)
  - Ubuntu Linux 10.04
  - Windows XP + Cygwin 1.7.x
  - Windows XP (Cygwin無し)
  - Windows 7 64bit + Cygwin 1.7.x
  - Windows 7 64bit (Cygwin無し)

ビルド時に利用したJDKのバージョンは以下のとおりです。

  - JDK 7u11

Groovyのバージョンは以下のとおりです。

  - Groovy 2.0以降

実装言語
--------

サーバサイドはJavaとGroovy、クライアントはC言語クライアントおよびRubyのクライアントが現在動作しています。また、サーバサイドではJNA(Java Native Access)を使用しています。

使い方
------

groovyコマンドの代わりにgroovyclientコマンドを実行します。::

    $ groovyclient -e "println('Hello, GroovyServ.')"
    Hello, GroovyServ.

groovyclientを実行したとき、groovyserverが起動されていなければ、バックグラウンドでgroovyserverが起動されます。起動されていない場合、起動のために数秒の待ち時間の後、サーバが起動し、実行が行われます。

明示的にgroovyserverを起動しておくこともできます。::

  $ groovyserver

起動オプションに-vを指定するとログファイルに詳細メッセージが出力されます。起動トラブルなどの解析に便利です。::

  $ groovyserver -v

その他の起動オプションについては後述します。

制限・機能の違い
----------------

* 異なるカレントディレクトリを同時に使うことはできません。複数のコンソールから実行した場合で、それぞれのコンソールで異なるカレントディレクトリで実行した場合も同じです。同時に実行中になることがなければ、異なるカレントディレクトリであっても、複数のコンソールから利用しても問題ありません。

  必要であれば別ポートで複数のGroovyServサーバを起動することもできます。
  別のサーバプロセスであれば、同時に異なるカレントディレクトリに対して処理を実行することができます。

* 静的変数はGroovyプログラム間の実行で共有されます。たとえば、システムプロパティが共有されます。::

    $ groovyclient -e "System.setProperty('a','abc')"
    $ groovyclient -e "println System.getProperty('a')"
    abc

  ただし、System.out／System.in／System.errはそれぞれのセッション毎に区別され、それぞれの標準入力／出力／エラー出力に接続されます。

* 環境変数は、通常、groovyclientコマンドを実行したときの値ではなく､groovyserverが起動されたときの値が使用されます。しかし、-Cenv、-Cenv-allオプションを指定することで、groovyclient実行時の環境変数の値をgroovyserver側に反映させることもできます。

  ただし環境変数CLASSPATHについては、これらのオプションを指定しなくても、毎回実行ごとにクライアント側の値が動的にサーバ側に反映されます。
  このクラスパスはセッションごとにクリアされて次回のスクリプトの実行に影響することはありません。

セキュリティ
------------

GroovyServサーバへの接続は、デフォルトではlocalhostからのみに制限されており、他のマシンからはgroovyclientコマンドを使って接続することはできません。
また、同マシン上でもgroovyserverを起動したユーザーと同じユーザが実行したgroovyclient の接続しか受け付けないように制約をかけています。
なお、この制約は、サーバが実行ごとに生成する秘密の認証トークンファイル~/.groovy/groovyserv/authtoken-<port>の内容を自ユーザのみが読み出せることに依存しています。
このファイルのアクセス制限を、UNIX環境ではowner以外からは読めないように設定(chmod 0400)していますが、Windows環境ではこの設定が機能しないため、必要に応じて他のユーザから読み出せないように設定してください。

v0.11から指定したクライアントマシンからのコマンド実行要求を受け付けることができるようになりました。

環境変数
--------

GroovyServは、実行時に以下の環境変数を使用します。

  HOME (LinuxまたはMac OS Xの場合のみ)
    認証トークンやPID、ログファイルを格納する~/.groovy/groovyserv ディレクトリを決定するために使用します。
    Unix系のOSであれば標準で設定されています。

  USERPROFILE (Windowsの場合のみ)
    認証トークンやPID、ログファイルを格納する~/.groovy/groovyservディレクトリを決定するために使用します。Windows標準で設定されています。
    BATファイルで起動された場合は、PIDファイルは作成されません。

  JAVA_HOME
    Groovyを実行するために必要です。
    通常はGroovyのインストール作業の一貫で設定されています。

  GROOVY_HOME (オプション)
    groovyコマンドのパスを特定するために使用します。
    groovyコマンドが環境変数PATHに設定されている場合はパス探索で見つけることができるため、環境変数GROOVY_HOMEは必要ありません。

  PATH中のgroovyコマンドパス (オプション)
    groovyコマンドのパスを特定するために使用します。
    環境変数GROOVY_HOMEが設定されている場合はそちらが優先して使用されるため、環境変数PATHへのgroovyコマンドの設定は必要ありません。

  GROOVYSERVER_PORT (オプション)
    サーバやクライアントでポート番号を指定する場合に使用します。
    コマンド引数で代替することもできます。

  CLASSPATH (オプション)
    groovyserverを起動したときの環境変数CLASSPATHは、そのままgroovyserverプロセスに引き継がれて、GroovyServとして必要なクラスパスが追加されたものがデフォルトクラスパスとして使われます。
    これは全てのスクリプト処理に影響します。

    groovyclientを起動したときの環境変数CLASSPATHは、毎回groovyserverへと転送され、サーバ上のスクリプトのコンパイル設定に動的に反映されます。
    (サーバ側の環境変数CLASSPATHに追加設定されるわけではない。)
    このクラスパスはセッションごとにクリアされるため、次回のスクリプトの実行に影響することはありません。スクリプト実行におけるクラスパスの探索では、groovyserver上の環境変数CLASSPATHが優先的に探索されます。
    なおこれらの振る舞いはgroovyclientの-cpオプションと全く同一です。

groovyclientのオプション
------------------------

groovyclientでは-Cで始まる以下の起動オプションが指定可能です。これらのオプションはgroovyclientで解釈され、groovyコマンドには渡されません。::

  -Ch,-Chelp                    このメッセージを表示する
  -Cs,-Chost                    接続するgroovyserverのネットワークアドレスを指定する
  -Cp,-Cport <port>             接続するgroovyserverのポート番号を指定する
  -Ck,-Ckill-server             起動中のgroovyserverを停止する
  -Ca,-Cauthtoken <authtoken>   認証トークンを指定する
  -Cr,-Crestart-server          起動中のgroovyserverを再起動する
  -Cq,-Cquiet                   起動時のメッセージを表示しない
  -Cenv <substr>                substrを変数名に含む環境変数をサーバに転送する
  -Cenv-all                     すべての環境変数の値をサーバに転送する
  -Cenv-exclude <substr>        substrを変数名に含む環境変数をサーバへの転送から除外する
  -Cv,-Cversion                 groovyclientのバージョンを表示する

groovyserverのオプション
------------------------

groovyserverの起動オプションは以下の通りです。::

  -v                       デバッグ情報などをログファイルに出力する
  -q                       起動時のメッセージを表示しない
  -k                       起動中のgroovyserverを終了する (groovyserver.batでは使えません)
  -r                       起動中のgroovyserverを再起動する (groovyserver.batでは使えません)
  -p <port>                LISTENするポート番号を指定する
  --allow-from <addresses> 追加でアクセスを許可するクライアントのIPアドレスを指定する(カンマ区切り) (groovyserver.batでは使えません)
  --authtoken <authtoken>  認証トークンを指定する (未指定の場合は自動的に生成します)

groovyserverの起動と停止
------------------------

groovyserverの起動方法には、groovyserverまたはgroovyserver.batを使って明示的に起動する方法と、groovyclientから透過的にバックグラウンドで起動する方法があります。

groovyserverを明示的に起動するためのコマンドは以下の通りです。

 - groovyserver      (Mac OS X, Linux, Windows(Cygwin))
 - groovyserver.bat  (Windows(Cygwinなし))

これらが利用可能な環境を以下に整理します。(OK: 利用可, N/A: 利用不可)

    =================  =================  ==================  ===============
    Script             Windows Cygwin版   Windows Cygwinなし  Mac OS X, Linux
    =================  =================  ==================  ===============
    groovyserver       OK                 N/A                 OK
    groovyserver.bat   OK                 OK                  N/A
    =================  =================  ==================  ===============

バッチファイル版groovyserver.batでは、技術的な理由により、-rオプションと-kオプションは利用できません。
このため、コマンドラインからはgroovyserverの終了と再起動を行うことができません。
その代わり、起動したgroovyserverは別ウィンドウで最小化されて実行されるため、Windowsの操作でウィンドウを閉じることでgroovyserverを終了することができます。
その後、groovyclientを実行することで、全体としてgroovyserverの再起動が可能です。

注意が必要なのは、Cygwin環境下でも、groovyclient.exeを経由したgroovyserverの透過的な起動の際には、内部的にgroovyserver.batが使用されるということです。
このため、Cygwin環境下では以下のような状況となります。

 - シェルスクリプト版groovyserverで明示的に起動した場合、同じくシェルスクリプト版groovyserverを-k,-rオプションを指定して実行することで、起動中のgroovyserverの終了と再起動を行うことができる。

 - バッチファイル版groovyserver.batで明示的に起動した場合、groovyserverの終了は、ウィンドウを閉じることで行う。

 - groovyclient.exeを通じてgroovyserver.batの透過的起動を行った場合、groovyserverの終了は、ウィンドウを閉じることで行う。

なお、シェルスクリプト版とバッチファイル版を問わず、透過的起動では、内部で起動するサーバにオプション(例えば-vオプション)を設定することはできません。デフォルト以外のオプションが必要な場合は、明示的な起動を行ってくださ
い。

環境変数の伝搬
--------------

groovyclientの-Cenvオプションを使うことで、指定した部分文字列が名前に含まれている環境変数をgroovyserverに転送することができます。
groovyclientプロセスにおけるこれらの環境変数の値はサーバプロセスに転送され、サーバプロセス上の同名の環境変数の値が上書きされます。
この機能はGroovyで書かれた外部コマンドを起動する際にパラメータを環境変数で受けわたすような仕様の既存ツール(IDE、TextMateなど)において特に有用です。

-Cenv-allオプションを指定すると、groovyclientプロセスのすべての環境変数がサーバ側に渡されます。
また-Cenv-excludeを併用することで、指定した部分文字列を変数名に含む環境変数を転送から除外することができます。

例えば、::

  -Cenv SUBSTRING

という指定をした場合、転送される環境変数の集合は以下のような疑似コードで決定されます。::

  allEnvironmentVariables.entrySet().findAll {
    it.name.contains("SUBSTRING")
  }

-Cenv／-Cenv-all／-Cenv-excludeを組み合わせたときのルールについては、例えば、::

  -Cenv SUBSTRING
  -Cenv-all
  -Cenv-exclude EXCLUDE_SUBSTRING

のとき、以下の疑似コードの結果がgroovyserverプロセスに送られることになります。::

  allEnvironmentVariables.entrySet().findAll {
    if (isSpecifiedEnvAll || it.name.contains("SUBSTRING")) {
      if (!it.name.contains("EXCLUDE_SUBSTRING")) {
        return true
      }
    }
    return false
  }

groovyserverプロセスに設定された環境変数は、groovyclientの終了後も値が残り続けることに注意してください。
また、groovyserverプロセスにおける環境変数の操作はスレッドセーフではありません。
複数のgroovyclientが同時に実行された場合、環境変数の値は後に起動されたgroovyclientによって上書きされるため、予期せぬ結果となる可能性があります。

ポート番号
----------

groovyserverとgroovyclientが通信するTCPポートとして、デフォルトでは、1961番ポートを使用します。
サーバが通信に使用するポート番号を変更するには、環境変数GROOVYSERVER_PORTを設定するか、-pオプションを指定してください。
環境変数と-pオプション両方が指定された場合は、-pオプションの値が優先されます。::

  $ export GROOVYSERVER_PORT=1963
  $ groovyserver

または::

  $ groovyserver -p 1963

クライアント側では環境変数GROOVYSERVER_PORT指定にくわえて-Cpオプションでポート番号を指定可能です。
透過的起動を行う場合にはgroovyserverに-pオプションが指定されて起動されます。::

  $ groovyclient -Cp 1963 -e '...'

ログファイル
------------

groovyserverのログは以下のファイルに出力されます。::

  ~/.groovy/groovyserv/groovyserver-<port>.log

リモートアクセス
----------------

まず許可するクライアントのアドレスを指定してgroovyserverを起動します。
この例ではサーバのIPアドレスを192.168.1.1、クライアントのIPアドレスを192.168.1.2とします。::

  server$ groovyserver --allow-from 192.168.1.2

次にそのクライアントからgroovyclientを実行します。
このとき~/.groovy/groovyserv/authtoken-<port>に格納された認証トークン文字列を、クライアント側で指定する必要があります。::

  server$ cat ~/.groovy/groovyserv/authtoken-1961
  7d3dc4d7a2b8b5ca

  client$ groovyclient -Chost 192.168.1.1 -Cauthtoken 7d3dc4d7a2b8b5ca -e "println('hello from remote client!!')"
  hello from remote client!!

認証トークンは、groovyserver起動時に明示的に指定することもできます。
ただし、総当たりや類推可能な文字列を指定した場合、セキュリティが低下することに注意してください。::

  server$ groovyserver --allow-from 192.168.1.2 --authtoken GROOVYSERV
  server$ cat ~/.groovy/groovyserv/authtoken-1961
  GROOVYSERV
  client$ groovyclient -Chost 192.168.1.1 -Cauthtoken GROOVYSERV -e "println('hello from remote client!!')"
  hello from remote client!!

groovyclientで-Chostオプションを指定した場合は、-Crなどのgroovyserver操作オプションは利用できません。
また、--allow-fromオプションでは複数のクライアントアドレスをカンマ区切りで指定することもできます。

Tips
----

groovyコマンドを実行すると代わりにgroovyclientが呼び出されるように、以下のようにエイリアス(別名)指定を行っておくと便利です。
以下はbash用のエイリアスの設定です。::

  alias groovy=groovyclient

Windowsではdoskeyコマンドで以下のように設定することができます。::

  doskey groovy=groovyclient $*

