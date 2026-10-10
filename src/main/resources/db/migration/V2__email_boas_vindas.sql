ALTER TABLE email_notificacao ADD COLUMN usuario_id bigint UNIQUE REFERENCES usuario(id);
ALTER TABLE email_notificacao ADD COLUMN nome_destinatario varchar(255);
