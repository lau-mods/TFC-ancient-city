const { scenario, command, camera, screenshot } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];

  command(client, 'time set noon');
  command(client, 'weather clear');
  screenshot(client, 'AC-F001-001', 'dedicated-server-client-startup');

  command(client, 'fill -4 -61 -1 3 -61 3 minecraft:stone');
  command(client, 'setblock -2 -60 1 tfcancientcity:rock/loose/deepslate[count=1]');
  command(client, 'setblock -1 -60 1 tfcancientcity:rock/loose/deepslate[count=2]');
  command(client, 'setblock 0 -60 1 tfcancientcity:rock/loose/deepslate[count=3]');
  command(client, 'setblock 1 -60 1 tfcancientcity:rock/loose/deepslate[count=4]');
  command(client, 'item replace entity @s hotbar.0 with tfcancientcity:rock/loose/deepslate 16');
  command(client, 'item replace entity @s hotbar.1 with tfcancientcity:brick/deepslate 16');

  await camera(client, { x: -0.5, y: -60, z: -2.5, yaw: 0, pitch: 25 }, 5_000);
  screenshot(client, 'AC-F005-003', 'counts-one-through-four-and-items');
});
