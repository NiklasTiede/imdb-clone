import ast
from pathlib import Path

PACKAGE_ROOT = Path(__file__).resolve().parents[1] / "src" / "imdb_agent"


def imported_modules(path: Path) -> set[str]:
    tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
    imports: set[str] = set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            imports.update(alias.name for alias in node.names)
        elif isinstance(node, ast.ImportFrom) and node.module is not None:
            imports.add(node.module)
    return imports


def python_modules(directory: Path) -> list[Path]:
    return sorted(path for path in directory.rglob("*.py") if path.name != "__init__.py")


def test_concierge_module_does_not_import_framework_or_adapters() -> None:
    forbidden_prefixes = (
        "fastapi",
        "pydantic_ai",
        "imdb_agent.adapters",
        "imdb_agent.web",
    )

    for path in python_modules(PACKAGE_ROOT / "concierge"):
        imports = imported_modules(path)
        violations = {imported for imported in imports if imported.startswith(forbidden_prefixes)}
        assert not violations, f"{path.relative_to(PACKAGE_ROOT)} imports {sorted(violations)}"


def test_web_adapter_does_not_import_outbound_adapters() -> None:
    for path in python_modules(PACKAGE_ROOT / "web"):
        imports = imported_modules(path)
        violations = {
            imported for imported in imports if imported.startswith("imdb_agent.adapters")
        }
        assert not violations, f"{path.relative_to(PACKAGE_ROOT)} imports {sorted(violations)}"


def test_only_bootstrap_assembles_web_and_outbound_adapters() -> None:
    for path in python_modules(PACKAGE_ROOT):
        imports = imported_modules(path)
        imports_web = any(imported.startswith("imdb_agent.web") for imported in imports)
        imports_adapters = any(imported.startswith("imdb_agent.adapters") for imported in imports)
        if imports_web and imports_adapters:
            assert path == PACKAGE_ROOT / "bootstrap.py"
