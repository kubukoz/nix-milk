{
  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.flake-utils.url = "github:numtide/flake-utils";
  inputs.sbt-derivation.url = "github:zaninime/sbt-derivation";
  inputs.gitignore-source.url = "github:hercules-ci/gitignore.nix";
  inputs.gitignore-source.inputs.nixpkgs.follows = "nixpkgs";

  outputs = { self, nixpkgs, flake-utils, sbt-derivation, ... }@inputs:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ sbt-derivation.overlay ];
        };
      in
        {
          defaultPackage = pkgs.callPackage ./derivation.nix { inherit (inputs) gitignore-source; };
          checks = {
            test-app =
              pkgs.runCommandNoCC "tests" { buildInputs = [ (pkgs.callPackage ./derivation.nix { inherit (inputs) gitignore-source; }) ]; } "nix-milk test > $out";
          };
        }
    );
}
