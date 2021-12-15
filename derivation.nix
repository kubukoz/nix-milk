{ jre, sbt, gitignore-source, makeWrapper, stdenv, lib, nix, coreutils, gawk }:

sbt.mkDerivation rec {
  pname = "nix-milk";
  version = "0.1.0";
  depsSha256 = "sha256-cUJefDiT0BuPiM2Hw68PxwcawZ0OK8tUU40y3KumqXA=";

  depsWarmupCommand = ''
    sbt Test/compile
  '';

  nativeBuildInputs = [ makeWrapper ];

  overrideDepsAttrs = _: {
    src = lib.sourceByRegex (gitignore-source.lib.gitignoreSource ./.) [
      "^project\$"
      "^project/.*\$"
      "^build.sbt\$"
    ];
  };
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
      --prefix PATH : "${lib.makeBinPath [ nix jre coreutils gawk ]}"
  '';
}
