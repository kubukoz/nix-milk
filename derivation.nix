{ jre, sbt, gitignore-source, makeWrapper, stdenv, lib, nix, coreutils }:

sbt.mkDerivation rec {
  pname = "nix-milk";
  version = "0.1.0";
  depsSha256 =
    if stdenv.isDarwin
    then "sha256-yoFpOYhOV1GRZ0IgGt3TiInz89NKupxYdSbPu1e5hI8="
    else "sha256-yoFpOYhOV1GRZ0IgGt3TiInz89NKupxYdSbPu1e5hI8=";

  depsWarmupCommand = ''
    sbt compile
  '';

  nativeBuildInputs = [ makeWrapper ];

  src = lib.sourceByRegex (gitignore-source.lib.gitignoreSource ./.) [
    "^project\$"
    "^project/.*\$"
    "^src\$"
    "^src/.*\$"
    "^build.sbt\$"
  ];

  buildPhase = ''
    sbt stage
  '';

  installPhase = ''
    mkdir -p $out/bin
    cp -r target/universal/stage/lib $out
    cp target/universal/stage/bin/root $out/bin/${pname}
    wrapProgram $out/bin/${pname} \
      --prefix PATH : "${nix}/bin:${jre}/bin:${coreutils}/bin"
  '';
}
