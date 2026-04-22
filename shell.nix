{ pkgs ? import <nixpkgs> { } }:

let
  jdk = pkgs.jdk11;
in
pkgs.mkShell {
  packages = [
    jdk
    pkgs.maven
    pkgs.docker-client
  ];

  JAVA_HOME = jdk.home;
}
