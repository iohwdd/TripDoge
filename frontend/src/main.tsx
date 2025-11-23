import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import router from './router'
import AppInitializer from './components/common/AppInitializer'
import './styles/index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppInitializer>
      <RouterProvider router={router} />
    </AppInitializer>
  </StrictMode>
)
