{ jre, sbt, gitignore-source, makeWrapper, stdenv }:

let
  mainClass = "com.kubukoz.nixmilk.Hello";
in
sbt.mkDerivation rec {
  pname = "sbt-nix-nix-milk";
  version = "0.1.0";
  depsSha256 =
    if stdenv.isDarwin
    then "sha256-iMNRM0zH/Yt3mE5ZOZJwsaZbPowIL0pWzoThUbiUf6c="
    else "sha256-2pQFwmP7lqF7fAWUGbvXfcxgMOrIchdT5TkhdE24yOg=";

  depsWarmupCommand = ''
    sbt compile
  '';

  nativeBuildInputs = [ makeWrapper ];

  src = gitignore-source.lib.gitignoreSource ./.;

  buildPhase = ''
    sbt stage
  '';

  installPhase = ''
    mkdir -p $out/{bin,lib}
    cp -ar target/universal/stage/lib $out/lib/${pname}
    makeWrapper ${jre}/bin/java $out/bin/${pname} \
      --add-flags "-cp '$out/lib/${pname}/*' '${mainClass}'"
  '';
}
