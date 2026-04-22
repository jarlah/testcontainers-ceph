{
  description = "testcontainers-ceph — Java development shell";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        jdk = pkgs.jdk11;
      in
      {
        devShells.default = pkgs.mkShell {
          packages = [
            jdk
            pkgs.maven
            pkgs.docker-client
          ];

          shellHook = ''
            export JAVA_HOME=${jdk.home}
          '';
        };
      });
}
