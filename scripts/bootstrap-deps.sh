#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# bootstrap-deps.sh
#
# Downloads Spring AI 1.0.0 artifacts directly from Maven Central and installs
# them into the local Maven cache (~/.m2/repository).
#
# Run this ONCE from any machine / network that can reach Maven Central:
#   chmod +x scripts/bootstrap-deps.sh && ./scripts/bootstrap-deps.sh
#
# After that, ./mvnw spring-boot:run works on the corporate network because
# the artifacts are served from the local cache (not re-downloaded).
# -----------------------------------------------------------------------------
set -euo pipefail

VERSION="1.0.0"
CENTRAL="https://repo.maven.apache.org/maven2/org/springframework/ai"
TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

install_artifact() {
  local artifact=$1
  local url_base="$CENTRAL/$artifact/$VERSION"

  echo "→ Downloading $artifact:$VERSION ..."
  curl -sSfL "$url_base/$artifact-$VERSION.jar" -o "$TMPDIR/$artifact.jar"
  curl -sSfL "$url_base/$artifact-$VERSION.pom" -o "$TMPDIR/$artifact.pom"

  mvn install:install-file \
    -Dfile="$TMPDIR/$artifact.jar" \
    -DpomFile="$TMPDIR/$artifact.pom" \
    -DgroupId=org.springframework.ai \
    -DartifactId="$artifact" \
    -Dversion="$VERSION" \
    -Dpackaging=jar \
    -q
  echo "  ✓ Installed $artifact:$VERSION"
}

# Core Spring AI artifacts needed by this project
ARTIFACTS=(
  spring-ai-bom
  spring-ai-core
  spring-ai-ollama
  spring-ai-ollama-spring-boot-autoconfigure
  spring-ai-ollama-spring-boot-starter
  spring-ai-milvus-store
  spring-ai-milvus-store-spring-boot-autoconfigure
  spring-ai-milvus-store-spring-boot-starter
  spring-ai-spring-boot-autoconfigure
)

echo "Installing Spring AI $VERSION artifacts into local Maven cache..."
echo "Using: $CENTRAL"
echo ""

for artifact in "${ARTIFACTS[@]}"; do
  install_artifact "$artifact"
done

echo ""
echo "Done. You can now run: ./mvnw spring-boot:run"
