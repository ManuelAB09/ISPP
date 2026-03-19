import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

const CalendarCallback = () => {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()

    useEffect(() => {
        const connected = searchParams.get('connected')
        const error = searchParams.get('error')
        navigate('/perfil', {
            state: {
                openSettings: true,
                calendarNotification: connected === 'true' ? 'success' : (error || 'error'),
            },
            replace: true,
        })
    }, []) // eslint-disable-line

    return (
        <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '100vh',
            fontFamily: 'inter, sans-serif',
            fontSize: '16px',
            color: '#666',
        }}>
            Conectando Google Calendar...
        </div>
    )
}

export default CalendarCallback
