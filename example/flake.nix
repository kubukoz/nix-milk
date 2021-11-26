{
  inputs.vetur.url = "http://localhost:8080/vscode-extensions/octref/vetur/latest.zip";
  outputs = { self, nixpkgs, vetur }: let
    pkgs = import nixpkgs {
      system = "x86_64-darwin";
      overlays = [ vetur.overlay ];
    };
  in
    {
      defaultPackage.x86_64-darwin = pkgs.vscode-extensions.octref.vetur;
    };

}
