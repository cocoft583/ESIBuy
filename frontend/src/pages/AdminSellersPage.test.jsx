import { describe, it, expect } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'
import { abrirSesionPorApi, renderApp } from '../test/renderApp'

const ADMIN = 'admin@esibuy.test'

async function abrirListado() {
  await abrirSesionPorApi(ADMIN)
  renderApp('/admin/sellers')
  await screen.findByRole('heading', { level: 1, name: /vendedores/i })
}

function filasDeDatos() {
  return within(screen.getByRole('table')).getAllByRole('row').slice(1)
}

async function esperarFilas(cantidad) {
  await waitFor(() => expect(filasDeDatos()).toHaveLength(cantidad))
}

describe('AdminSellersPage (HU26): acceso desde el panel', () => {
  it('adminPage_alPulsarVendedores_navegaAlListadoDeVendedores', async () => {
    const user = userEvent.setup()
    await abrirSesionPorApi(ADMIN)
    renderApp('/admin')

    await user.click(await screen.findByRole('button', { name: /vendedores/i }))

    await waitFor(() => expect(screen.getByTestId('ruta-actual')).toHaveTextContent(/^\/admin\/sellers$/))
  })

  it('adminSellersPage_alPulsarVolverAlPanel_regresaAlPanelDeControl', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.click(screen.getByRole('link', { name: /volver al panel/i }))

    await waitFor(() => expect(screen.getByTestId('ruta-actual')).toHaveTextContent(/^\/admin$/))
  })

  it('adminSellersPage_sinSesion_redirigeAlLogin', async () => {
    renderApp('/admin/sellers')

    await waitFor(() => expect(screen.getByTestId('ruta-actual')).toHaveTextContent(/^\/login$/))
  })

  it('adminSellersPage_conSesionDeVendedor_noMuestraElListado', async () => {
    await abrirSesionPorApi('vendedor@esibuy.test')
    renderApp('/admin/sellers')

    await waitFor(() => expect(screen.getByTestId('ruta-actual')).toHaveTextContent(/^\/seller\/products$/))
    expect(screen.queryByRole('heading', { name: /^vendedores$/i })).not.toBeInTheDocument()
  })
})

describe('AdminSellersPage (HU26): listado', () => {
  it('adminSellersPage_muestraLaPrimeraPaginaConDiezVendedores', async () => {
    await abrirListado()

    await esperarFilas(10)
    expect(screen.getByText('TecnoVera')).toBeInTheDocument()
    expect(screen.getByText(/mostrando 1–10 de 14 vendedores/i)).toBeInTheDocument()
  })

  it('adminSellersPage_muestraNombreCorreoCategoriaYEstadoDeCadaVendedor', async () => {
    await abrirListado()

    const fila = (await screen.findByText('TecnoVera')).closest('tr')
    expect(within(fila).getByText('Vera Vendedora')).toBeInTheDocument()
    expect(within(fila).getByText('vendedor@esibuy.test')).toBeInTheDocument()
    expect(within(fila).getByText('Tecnología')).toBeInTheDocument()
    expect(within(fila).getByText('Activo')).toBeInTheDocument()
  })

  it('adminSellersPage_distingueLosEstadosPendienteYDesactivado', async () => {
    await abrirListado()

    expect(within((await screen.findByText('Hogar Hugo')).closest('tr')).getByText('Pendiente')).toBeInTheDocument()
    expect(within(screen.getByText('Cocina Pablo').closest('tr')).getByText('Desactivado')).toBeInTheDocument()
  })

  it('adminSellersPage_cadaFilaTieneUnBotonConLupaParaVerSuInformacion', async () => {
    await abrirListado()

    await esperarFilas(10)
    expect(screen.getAllByRole('button', { name: /ver información de/i })).toHaveLength(10)
    expect(screen.getByRole('button', { name: /ver información de tecnovera/i })).toBeInTheDocument()
  })

  it('adminSellersPage_noOfreceAccionesDeEdicionNiBorradoEnLasFilas', async () => {
    await abrirListado()

    await esperarFilas(10)
    expect(screen.queryByRole('button', { name: /editar|eliminar|borrar/i })).not.toBeInTheDocument()
  })

  it('adminSellersPage_conErrorDeServidor_muestraMensajeSinDetallesInternosYPermiteReintentar', async () => {
    server.use(
      http.get(
        '/api/admin/sellers',
        () => HttpResponse.json({ message: 'MongoTimeoutException' }, { status: 500 }),
        { once: true },
      ),
    )
    const user = userEvent.setup()
    await abrirSesionPorApi(ADMIN)
    renderApp('/admin/sellers')

    expect(await screen.findByText(/no se han podido cargar los vendedores/i)).toBeInTheDocument()
    expect(screen.queryByText(/mongotimeout/i)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /reintentar/i }))

    await esperarFilas(10)
  })

  it('adminSellersPage_mientrasCarga_indicaQueEstaCargando', async () => {
    await abrirSesionPorApi(ADMIN)
    renderApp('/admin/sellers')

    expect(await screen.findByText(/cargando vendedores/i)).toBeInTheDocument()
    await esperarFilas(10)
  })

  it('adminSellersPage_sinVendedoresRegistrados_muestraElEstadoVacio', async () => {
    server.use(
      http.get('/api/admin/sellers', () =>
        HttpResponse.json({ items: [], page: 1, size: 10, totalItems: 0, totalPages: 0 }),
      ),
    )
    await abrirSesionPorApi(ADMIN)
    renderApp('/admin/sellers')

    expect(await screen.findByRole('heading', { name: /todavía no hay vendedores/i })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })
})

describe('AdminSellersPage (HU26): filtros', () => {
  it('adminSellersPage_alBuscarPorNombreComercial_filtraSinPulsarNingunBoton', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.type(await screen.findByLabelText(/buscar por nombre/i), 'tecnovera')

    await esperarFilas(1)
    expect(screen.getByText('TecnoVera')).toBeInTheDocument()
  })

  it('adminSellersPage_laBusqueda_ignoraMayusculasYAcentos', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.type(await screen.findByLabelText(/buscar por nombre/i), 'LUCIA')

    await esperarFilas(1)
    expect(screen.getByText('Lucía Digital')).toBeInTheDocument()
  })

  it('adminSellersPage_laBusqueda_tambienEncuentraPorCorreo', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.type(await screen.findByLabelText(/buscar por nombre/i), 'hugo@esibuy')

    await esperarFilas(1)
    expect(screen.getByText('Hogar Hugo')).toBeInTheDocument()
  })

  it('adminSellersPage_alFiltrarPorCategoria_muestraSoloLosVendedoresDeEsaCategoria', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.selectOptions(await screen.findByLabelText(/categoría/i), 'Hogar')

    await esperarFilas(7)
    filasDeDatos().forEach((fila) => expect(within(fila).getByText('Hogar')).toBeInTheDocument())
  })

  it('adminSellersPage_combinaBusquedaYCategoria', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.selectOptions(await screen.findByLabelText(/categoría/i), 'Hogar')
    await user.type(screen.getByLabelText(/buscar por nombre/i), 'hugo')

    await esperarFilas(1)
    expect(screen.getByText('Hogar Hugo')).toBeInTheDocument()
  })

  it('adminSellersPage_sinCoincidencias_ofreceLimpiarLosFiltros', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.type(await screen.findByLabelText(/buscar por nombre/i), 'zzzz')
    expect(await screen.findByText(/no hay vendedores que coincidan/i)).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /todavía no hay vendedores/i })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /limpiar filtros/i }))

    await esperarFilas(10)
    expect(screen.getByLabelText(/buscar por nombre/i)).toHaveValue('')
  })
})

describe('AdminSellersPage (HU26): paginación', () => {
  it('adminSellersPage_alPulsarSiguiente_muestraLaSegundaPagina', async () => {
    const user = userEvent.setup()
    await abrirListado()
    await esperarFilas(10)

    await user.click(screen.getByRole('button', { name: /siguiente/i }))

    expect(await screen.findByText('Bravo Audio')).toBeInTheDocument()
    expect(screen.queryByText('TecnoVera')).not.toBeInTheDocument()
    expect(screen.getByText(/página 2 de 2/i)).toBeInTheDocument()
    await esperarFilas(4)
  })

  it('adminSellersPage_alFiltrarDesdeOtraPagina_vuelveALaPrimera', async () => {
    const user = userEvent.setup()
    await abrirListado()
    await esperarFilas(10)

    await user.click(screen.getByRole('button', { name: /siguiente/i }))
    await screen.findByText(/página 2 de 2/i)
    await user.type(screen.getByLabelText(/buscar por nombre/i), 'a')

    await waitFor(() => expect(screen.queryByText(/página 2 de 2/i)).not.toBeInTheDocument())
  })
})

describe('AdminSellersPage (HU26): información del vendedor', () => {
  it('adminSellersPage_alPulsarLaLupa_abreLaInformacionEncimaDelListado', async () => {
    const user = userEvent.setup()
    await abrirListado()

    await user.click(await screen.findByRole('button', { name: /ver información de tecnovera/i }))

    const dialogo = await screen.findByRole('dialog', { name: /información del vendedor/i })
    expect(await within(dialogo).findByLabelText('Nombre comercial')).toHaveValue('TecnoVera')
    expect(screen.getByTestId('ruta-actual')).toHaveTextContent(/^\/admin\/sellers$/)
    expect(screen.getByRole('table', { hidden: true })).toBeInTheDocument()
  })

  it('adminSellersPage_alCerrarLaInformacion_vuelveAlListadoSinCambiarDeRuta', async () => {
    const user = userEvent.setup()
    await abrirListado()
    await user.click(await screen.findByRole('button', { name: /ver información de tecnovera/i }))
    const dialogo = await screen.findByRole('dialog', { name: /información del vendedor/i })

    await user.click(within(dialogo).getByRole('button', { name: /cerrar/i }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByTestId('ruta-actual')).toHaveTextContent(/^\/admin\/sellers$/)
    expect(screen.getByRole('table')).toBeInTheDocument()
  })

  it('adminSellersPage_alAbrirOtroVendedor_muestraSusDatos', async () => {
    const user = userEvent.setup()
    await abrirListado()
    await user.click(await screen.findByRole('button', { name: /ver información de casa vicente/i }))
    const dialogo = await screen.findByRole('dialog', { name: /información del vendedor/i })

    expect(await within(dialogo).findByLabelText('Nombre comercial')).toHaveValue('Casa Vicente')
    expect(within(dialogo).getByLabelText('Categoría principal')).toHaveValue('Hogar')
  })
})

describe('AdminSellersPage (HU26): el servidor protege los datos', () => {
  it('adminSellersApi_conSesionDeVendedor_respondeProhibido', async () => {
    await abrirSesionPorApi('vendedor@esibuy.test')

    const respuesta = await fetch('/api/admin/sellers', { credentials: 'include' })

    expect(respuesta.status).toBe(403)
  })
})
