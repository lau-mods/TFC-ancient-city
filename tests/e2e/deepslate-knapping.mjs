const { scenario, command, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];

  command(client, 'gamemode creative');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'item replace entity @s hotbar.0 with tfcancientcity:rock/loose/deepslate 2');
  mct(client, 'inventory', 'hotbar', '0');
  mct(client, 'inventory', 'use');
  const gui = await waitFor(() => mct(client, 'gui', 'info'), data => data.type === 'KnappingScreen', 5_000);
  if (gui.type !== 'KnappingScreen') throw new Error(`Loose Deepslate did not open KnappingScreen: ${JSON.stringify(gui)}`);
  screenshot(client, 'AC-F007-004', 'deepslate-knapping-material');
});
