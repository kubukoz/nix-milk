{
  outputs = { self }:
    {
      overlay = final: prev: {
        vscode-extensions = prev.vscode-utils.extensionFromVscodeMarketplace {
          name = "???";
          publisher = "???";
          version = "???";
          sha256 = "";
        };
      };
    };
}
