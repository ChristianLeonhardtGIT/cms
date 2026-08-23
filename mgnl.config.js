import CreatePagePlugin from '@magnolia/cli-create-page-plugin';
import CreateComponentPlugin from '@magnolia/cli-create-component-plugin';
import CreateVirtualUriPlugin from '@magnolia/cli-create-virtual-uri-plugin';
import CreateContentTypePlugin from '@magnolia/cli-create-content-type-plugin';
import CreateAppPlugin from '@magnolia/cli-create-app-plugin';
import CreateLightModulePlugin from '@magnolia/cli-create-light-module-plugin';
import StartPlugin from '@magnolia/cli-start-plugin';
export default {
	lightModule: 'meine-website',
	analytics: {
		enabled: false,
		// Set to false to turn off analytics
		uuid: '09f2e2c9-b9d7-4d83-8d46-8f96475465ff',
	},
	// Logger configuration
	// see: https://github.com/winstonjs/winston#logging for logging levels explanation
	logger: {
		filename: './mgnl.error.log',
		fileLevel: 'debug',
		consoleLevel: 'info',
	},
	// Here you can add plugins you want to use with MGNL CLI
	plugins: [
		new CreatePagePlugin({
			lightModule: 'meine-website',
			framework: '@magnolia/cli-freemarker-prototypes',
		}),
		new CreateComponentPlugin({
			lightModule: 'meine-website',
			framework: '@magnolia/cli-freemarker-prototypes',
		}),
		new CreateVirtualUriPlugin(),
		new CreateContentTypePlugin(),
		new CreateAppPlugin(),
		new CreateLightModulePlugin({}),
		new StartPlugin(),
	],
	lightModulesPath: './light-modules',
};
