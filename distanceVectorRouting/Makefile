# Java compiler
JC = javac

# Java runtime
JAVA = java

# Source directory
SRCDIR = src

# Build directory
BUILDDIR = bin

# Package name
PACKAGE = distanceVectorRouting

# Main class
MAIN = dv

# Source files
SOURCES = $(SRCDIR)/$(PACKAGE)/$(MAIN).java

# Class files
CLASSES = $(BUILDDIR)/$(PACKAGE)/$(MAIN).class

# Default target
all: $(BUILDDIR) $(CLASSES)

# Create build directory if it doesn't exist
$(BUILDDIR):
	mkdir -p $(BUILDDIR)/$(PACKAGE)

# Compile Java files
$(CLASSES): $(SOURCES)
	$(JC) -d $(BUILDDIR) $(SOURCES)

# Clean built files
clean:
	rm -rf $(BUILDDIR)

# Run the program
# Usage: make run TOPO=<topology-file> INTERVAL=<update-interval>
# Example: make run TOPO=topology1.txt INTERVAL=10
run: $(CLASSES)
	$(JAVA) -cp $(BUILDDIR) $(PACKAGE).$(MAIN) -t $(TOPO) -i $(INTERVAL)

# Help target
help:
	@echo "Distance Vector Routing Program Makefile"
	@echo "Available targets:"
	@echo "  make all      - Compile the program"
	@echo "  make clean    - Remove compiled files"
	@echo "  make run      - Run the program"
	@echo "  make help     - Show this help message"
	@echo ""
	@echo "To run the program:"
	@echo "  make run TOPO=<topology-file> INTERVAL=<update-interval>"
	@echo "Example:"
	@echo "  make run TOPO=topology1.txt INTERVAL=10"

.PHONY: all clean run help