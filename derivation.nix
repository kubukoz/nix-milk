{ jre, sbt, gitignore-source, makeWrapper, stdenv }:

let
  mainClass = "com.kubukoz.nixmilk.Hello";
in
sbt.mkDerivation rec {
  pname = "sbt-nix-nix-milk";
  version = "0.1.0";
  depsSha256 =
    if stdenv.isDarwin
    then "sha256-qk+fPbxdiRSjoE3KoXZ8eN958QoTXmHj2ooOn6vuPdg="
    else "sha256-3Bh0mnJ6SrnOjmeHmSMLKLqE1uePK0eKdAFTuK8H9UM=";

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
