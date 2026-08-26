import { DOCUMENT_TYPES } from '../../api/identityApi.js'

export default function DocumentTypeSelect({ value, onChange, disabled }) {
  return (
    <div>
      <label htmlFor="documentType" className="field-label">
        Document type
        <span className="ml-0.5 text-teal-700">*</span>
      </label>
      <select
        id="documentType"
        className="input-field"
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="" disabled>
          Select a document type
        </option>
        {DOCUMENT_TYPES.map((doc) => (
          <option key={doc.value} value={doc.value}>
            {doc.label}
          </option>
        ))}
      </select>
    </div>
  )
}
