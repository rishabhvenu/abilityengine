# AbilityEngine Documentation Site

This directory contains the MKDocs documentation site for AbilityEngine.

## Setup

1. Install Python 3.8+ if not already installed

2. Install dependencies:

```bash
pip install -r requirements.txt
```

## Building & Serving

### Local Development Server

```bash
mkdocs serve
```

Then visit `http://localhost:8000` in your browser. The site will auto-reload when you edit files.

### Build Static Site

```bash
mkdocs build
```

Output will be in the `site/` directory.

## Deploying

### GitHub Pages

```bash
mkdocs gh-deploy
```

This will build the site and push it to the `gh-pages` branch.

### Manual Deployment

Build the site and copy the `site/` directory contents to your web server.

## Documentation Structure

```
docs/
├── index.md                    # Home page
├── getting-started/
│   ├── installation.md
│   └── quick-start.md
├── guides/
│   ├── yaml-abilities.md
│   ├── scripting.md
│   ├── module-development.md
│   ├── sessions.md
│   └── items.md
├── reference/
│   ├── api/                    # 10 API interface pages
│   ├── yaml-schema.md
│   ├── actions.md
│   ├── scripting-api.md
│   ├── commands.md
│   └── trigger-reference.md
├── architecture/
│   ├── overview.md
│   └── internals.md
└── examples/
    ├── yaml-examples.md
    └── script-examples.md
```

## Editing Documentation

All documentation is written in Markdown. Edit the `.md` files in the `docs/` directory.

### Markdown Extensions

This site uses Material for MKDocs with the following extensions:

- **Code highlighting** with line numbers
- **Admonitions** for tips, warnings, notes
- **Tables** for structured data
- **Mermaid diagrams** for flowcharts and diagrams
- **Tabbed content** for multi-format examples
- **Task lists** for checklists

### Adding a New Page

1. Create a new `.md` file in the appropriate `docs/` subdirectory
2. Add it to the `nav` section in `mkdocs.yml`
3. Run `mkdocs serve` to preview

## Configuration

Edit `mkdocs.yml` to configure:

- Site metadata (name, description, author)
- Theme settings (colors, features)
- Navigation structure
- Markdown extensions
- Plugins

## Requirements

See `requirements.txt` for Python dependencies:

- mkdocs >= 1.5.0
- mkdocs-material >= 9.5.0
- pymdown-extensions >= 10.7

## Support

For issues with the documentation site, check:

1. MKDocs documentation: https://www.mkdocs.org/
2. Material for MKDocs: https://squidfunk.github.io/mkdocs-material/
3. Project issues: https://github.com/yourusername/abilityengine/issues
