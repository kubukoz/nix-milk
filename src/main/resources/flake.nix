{
  outputs = { self }:
    {
      overlay = final: prev: {
        vscode-extensions = prev.lib.recursiveUpdate prev.vscode-extensions {
          TEMPLATE_PUBLISHER = {
            TEMPLATE_NAME = prev.vscode-utils.extensionFromVscodeMarketplace {
              name = "TEMPLATE_NAME";
              publisher = "TEMPLATE_PUBLISHER";
              version = "TEMPLATE_VERSION";
              sha256 = "TEMPLATE_SHA256";
            };
          };
        };
      };
    };
}
