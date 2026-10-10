import { useState } from 'react'
import { request } from './api'

export default function PasswordRecovery({ Modal, resetToken, onClose, onLogin, onRequestNew, onReset }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const resetting = Boolean(resetToken)
  async function submit(event) {
    event.preventDefault(); setError('')
    if (resetting && password !== confirmation) { setError('As senhas precisam ser iguais.'); return }
    if (resetting && !password.trim()) { setError('Digite uma senha com pelo menos 8 caracteres.'); return }
    setBusy(true)
    try {
      await request(resetting ? '/auth/redefinir-senha' : '/auth/recuperar-senha', { method: 'POST', body: JSON.stringify(resetting ? { token: resetToken, novaSenha: password } : { email: email.trim() }) })
      if (resetting) onReset()
      setPassword(''); setConfirmation(''); setSuccess(true)
    } catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }
  return <Modal onClose={onClose} className="auth-modal recovery-modal">
    <button className="icon-button modal-close" onClick={onClose} aria-label="Fechar">×</button>
    <div className="auth-mark">c<span>.</span></div><p className="eyebrow">ACESSO À SUA CONTA</p>
    <h2>{success ? resetting ? 'Senha atualizada.' : 'Confira seu e-mail.' : resetting ? 'Escolha sua nova senha.' : 'Esqueceu sua senha?'}</h2>
    <p className="muted">{success ? resetting ? 'Entre novamente com a nova senha para continuar suas compras.' : 'Se houver uma conta com esse e-mail, enviaremos um link. Confira também a caixa de spam. O link vale por 30 minutos.' : resetting ? 'Use de 8 a 128 caracteres. A troca de senha encerra os acessos anteriores à conta.' : 'Informe o e-mail usado no cadastro para receber um link de recuperação.'}</p>
    {error && <p className="account-error" role="alert">{error}</p>}
    {!success && <form className="auth-form" onSubmit={submit}>
      {resetting ? <><label>Nova senha<input autoFocus required type="password" autoComplete="new-password" minLength={8} maxLength={128} value={password} onChange={event => setPassword(event.target.value)} /></label><label>Confirmar nova senha<input required type="password" autoComplete="new-password" minLength={8} maxLength={128} value={confirmation} onChange={event => setConfirmation(event.target.value)} /></label></> : <label>E-mail da conta<input autoFocus required type="email" autoComplete="email" maxLength={254} value={email} onChange={event => setEmail(event.target.value)} /></label>}
      <button className="button button-dark full" disabled={busy}>{busy ? 'Aguarde…' : resetting ? 'Salvar nova senha' : 'Enviar link de recuperação'}</button>
    </form>}
    <p className="auth-switch"><button onClick={onLogin} disabled={busy}>Voltar para entrar</button>{resetting && !success && <> · <button onClick={onRequestNew} disabled={busy}>Solicitar outro link</button></>}</p>
  </Modal>
}
