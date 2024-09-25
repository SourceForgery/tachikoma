package main

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
)

// Path to the gradle.properties file
const gradlePropertiesPath = "../gradle.properties"

func main() {
	// Check if protoc is installed
	_, err := exec.LookPath("protoc")
	if err != nil {
		fmt.Println("protoc not found. Installing...")

		// Read version from gradle.properties
		protocVersion, err := readProtocVersion()
		if err != nil {
			fmt.Println("Error reading protoc version:", err)
			return
		}

		// Install protoc with the specified version
		installProtoc(protocVersion)
	} else {
		fmt.Println("protoc already installed.")
	}

	// Run protoc command to generate Go files from .proto files
	cmd := exec.Command("protoc", "--go_out=.", "--go-grpc_out=.", "--proto_path=proto", "proto/*.proto")
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err = cmd.Run()
	if err != nil {
		fmt.Println("Error running protoc:", err)
	}
}

// readProtocVersion reads the protoc version from the gradle.properties file.
func readProtocVersion() (string, error) {
	file, err := os.Open(gradlePropertiesPath)
	if err != nil {
		return "", fmt.Errorf("failed to open gradle.properties: %w", err)
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, "protocVersion=") {
			version := strings.TrimPrefix(line, "protocVersion=")
			return strings.TrimSpace(version), nil
		}
	}

	if err := scanner.Err(); err != nil {
		return "", fmt.Errorf("error reading gradle.properties: %w", err)
	}

	return "", fmt.Errorf("protocVersion not found in gradle.properties")
}

// installProtoc downloads and installs the protoc compiler for the specified version.
func installProtoc(version string) {
	platform := fmt.Sprintf("%s-%s", runtime.GOOS, runtime.GOARCH)
	protocURL := fmt.Sprintf("https://github.com/protocolbuffers/protobuf/releases/download/v%s/protoc-%s-%s.zip", version, version, platform)

	// Print the download URL for debugging purposes
	fmt.Printf("Downloading protoc version %s from %s\n", version, protocURL)

	// Example install location; modify this to where protoc should be installed
	installPath := filepath.Join("../build/protoc")
	err := os.MkdirAllinstallPath, 0755)
	if err != nil {
		log.Fatalln( "Failed to create install directory", err)
	}
	fmt.Printf("Installing protoc to %s\n", installPath)

	http.Get(protocURL)

	// Here you'd implement the actual download and extraction of the zip file,
	// but this part depends on your environment and available tools.
	// You can use the `os/exec` package to call curl/wget and unzip, or
	// use Go's standard libraries to handle HTTP requests and unzip files.

	// For now, this is just a placeholder.
	fmt.Println("Installation logic should be implemented here.")
}
