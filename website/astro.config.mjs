// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import mermaid from 'astro-mermaid';
import starlightLinksValidator from 'starlight-links-validator';

// https://astro.build/config
export default defineConfig({
	site: 'https://evsinev.github.io',
	base: '/firewall-config',
	integrations: [
		// Must run before Starlight so ```mermaid fences render as diagrams (not code blocks).
		mermaid({ theme: 'default', autoTheme: true }),
		starlight({
			title: 'firewall-config',
			description:
				'Generate iptables rules, L2/L3 network diagrams and audit documentation from a YAML description of your network',
			customCss: ['./src/styles/mermaid.css'],
			plugins: [starlightLinksValidator()],
			social: [
				{ icon: 'github', label: 'GitHub', href: 'https://github.com/evsinev/firewall-config' },
			],
			sidebar: [
				{ label: 'Start here', items: ['index', 'installation', 'quick-start'] },
				{ label: 'Configuration', items: [{ autogenerate: { directory: 'configuration' } }] },
				{ label: 'Generators', items: [{ autogenerate: { directory: 'generators' } }] },
				{ label: 'Reference', items: [{ autogenerate: { directory: 'reference' } }] },
				{ label: 'Internals', items: [{ autogenerate: { directory: 'internals' } }] },
			],
		}),
	],
});
