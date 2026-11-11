const { scenario, command, camera, screenshot, waitFor, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];

  command(client, 'gamemode creative');
  command(client, 'time set noon');
  command(client, 'weather clear');
  command(client, 'fill -3 -61 -1 3 -61 4 minecraft:stone');
  command(client, 'fill -3 -60 -1 3 -56 4 minecraft:air');
  command(client, 'place template minecraft:ancient_city/e2e_chest 0 -60 1');

  await camera(client, { x: 0.5, y: -60, z: -2.5, yaw: 0, pitch: 10 });
  mct(client, 'block', 'interact', '0', '-60', '1');
  const gui = await waitFor(() => mct(client, 'gui', 'info'), data => data.type === 'ContainerScreen', 5_000);
  if (gui.type !== 'ContainerScreen') throw new Error('Replaced Ancient City Chest did not open');
  screenshot(client, 'AC-F004-003', 'blackwood-chest-generated-loot');
});
