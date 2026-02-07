.PHONY: build clean test publish release version bump-patch bump-minor bump-major help

# Default target
help:
	@echo "Stallwart Build Commands"
	@echo "========================"
	@echo "make build       - Build the library"
	@echo "make clean       - Clean build artifacts"
	@echo "make test        - Run tests"
	@echo "make publish     - Publish to Maven Central"
	@echo "make version     - Show current version"
	@echo "make bump-patch  - Bump patch version (1.0.0 -> 1.0.1)"
	@echo "make bump-minor  - Bump minor version (1.0.0 -> 1.1.0)"
	@echo "make bump-major  - Bump major version (1.0.0 -> 2.0.0)"
	@echo "make release     - Full release flow (build, test, publish)"

# Build the library
build:
	./gradlew :stallwart:assemble

# Clean build artifacts
clean:
	./gradlew clean

# Run tests
test:
	./gradlew :stallwart:test

# Show current version
version:
	@grep VERSION_NAME version.properties | cut -d'=' -f2

# Publish to Maven Central
publish:
	./gradlew publishAndReleaseToMavenCentral

# Bump patch version (1.0.0 -> 1.0.1)
bump-patch:
	@current=$$(grep VERSION_NAME version.properties | cut -d'=' -f2); \
	major=$$(echo $$current | cut -d. -f1); \
	minor=$$(echo $$current | cut -d. -f2); \
	patch=$$(echo $$current | cut -d. -f3); \
	new_patch=$$((patch + 1)); \
	new_version="$$major.$$minor.$$new_patch"; \
	sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$$new_version/" version.properties; \
	code=$$(grep VERSION_CODE version.properties | cut -d'=' -f2); \
	new_code=$$((code + 1)); \
	sed -i '' "s/VERSION_CODE=.*/VERSION_CODE=$$new_code/" version.properties; \
	echo "Bumped version: $$current -> $$new_version"

# Bump minor version (1.0.0 -> 1.1.0)
bump-minor:
	@current=$$(grep VERSION_NAME version.properties | cut -d'=' -f2); \
	major=$$(echo $$current | cut -d. -f1); \
	minor=$$(echo $$current | cut -d. -f2); \
	new_minor=$$((minor + 1)); \
	new_version="$$major.$$new_minor.0"; \
	sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$$new_version/" version.properties; \
	code=$$(grep VERSION_CODE version.properties | cut -d'=' -f2); \
	new_code=$$((code + 1)); \
	sed -i '' "s/VERSION_CODE=.*/VERSION_CODE=$$new_code/" version.properties; \
	echo "Bumped version: $$current -> $$new_version"

# Bump major version (1.0.0 -> 2.0.0)
bump-major:
	@current=$$(grep VERSION_NAME version.properties | cut -d'=' -f2); \
	major=$$(echo $$current | cut -d. -f1); \
	new_major=$$((major + 1)); \
	new_version="$$new_major.0.0"; \
	sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$$new_version/" version.properties; \
	code=$$(grep VERSION_CODE version.properties | cut -d'=' -f2); \
	new_code=$$((code + 1)); \
	sed -i '' "s/VERSION_CODE=.*/VERSION_CODE=$$new_code/" version.properties; \
	echo "Bumped version: $$current -> $$new_version"

# Full release flow
release: build test
	@echo ""
	@echo "Ready to publish version $$(make version)"
	@echo "Run 'make publish' to publish to Maven Central"
	@echo "Then: git add . && git commit -m 'Release $$(make version)'"
	@echo "Then: git tag v$$(make version) && git push && git push --tags"
