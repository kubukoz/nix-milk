# nix-milk

Add it to your flakes.

## Experimental

Warning: this project is experimental, use at your own risk. The hosted service is currently available at http://nix-milk.kubukoz.com,
but I may stop supporting it at any time if I consider it unworthy of the maintenance cost.

## What is this?

This is a realization of an experiment to bring more inputs to Nix Flakes. The idea is that Nix accepts any URL that ends with the right extension (e.g. `.zip` or `.tar.gz`),
and the output could be generated dynamically based on the exact URL.

## Usage

In your `flake.nix`, add the desired package as an input, e.g.:

```nix
{
  inputs.vetur.url = "https://nix-milk.kubukoz.com/vscode-extensions/octref/vetur/latest.zip";
}
```

The server will generate a zipfile with a `flake.nix` that contains an overlay that adds the package to `pkgs.vscode-extensions`, in the style of the default ones.
This takes quite a lot of time because we have to first query the Marketplace for the latest version, then get the sha of the package by the means of `nix-prefetch-url`.

Nix will only make a request to the URL if you build first time or explicitly request an update of inputs (or that particular input), so in normal operation this brings no overhead.

A complete example of usage:

```nix
{
  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.vetur.url = "https://nix-milk.kubukoz.com/vscode-extensions/octref/vetur/latest.zip";
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
```

## Possible roadmap

- [x] Hosting this somewhere in the public Internet
- [x] Latest version of a VS Code package
- [x] Caching shas for immutable URLs (e.g. a specific version of a VS code package)
- [ ] Specific version of a VS Code package
- [ ] Proxy to a limited number of "mostly immutable" URLs, e.g. GitHub releases with a size limit

## Development

- `nix develop`, or use the normal Scala+sbt workflow
- Run the app with `sbt run`
- Test your changes with `nix flake check`
- Binary caches may be available with `nix run nixpkgs#cachix use kubukoz`
