import React from 'react';
import './LinkPreviewCard.css';

const LinkPreviewCard = ({ preview }) => {
    if (!preview) {
        return null;
    }

    const href = preview.url || '#';
    const title = preview.title || preview.domain || href;
    const description = preview.description || '';
    const siteName = preview.siteName || preview.domain || '';

    return (
        <a
            className="link-preview-card"
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            title={title}
        >
            {preview.image ? (
                <img className="link-preview-image" src={preview.image} alt={title} loading="lazy" />
            ) : null}
            <div className="link-preview-body">
                <span className="link-preview-site">{siteName}</span>
                <strong className="link-preview-title">{title}</strong>
                {description ? <p className="link-preview-description">{description}</p> : null}
                <span className="link-preview-url">{preview.domain || href}</span>
            </div>
        </a>
    );
};

export default LinkPreviewCard;
