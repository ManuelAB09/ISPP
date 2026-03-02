
export default function CreateIcon({ width = 26, height = 26, fill = '#2B2B2B', stroke = '#2B2B2B', strokeWidth = 0.25 }) {
    return (
        <svg width={width} height={height} viewBox="0 0 26 26" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ cursor: "pointer" }}>
            <path d="M22.75 15.1667V20.5833C22.75 21.158 22.5217 21.7091 22.1154 22.1154C21.7091 22.5217 21.158 22.75 20.5833 22.75H5.41667C4.84203 22.75 4.29093 22.5217 3.8846 22.1154C3.47827 21.7091 3.25 21.158 3.25 20.5833V5.41667C3.25 4.84203 3.47827 4.29093 3.8846 3.8846C4.29093 3.47827 4.84203 3.25 5.41667 3.25H10.8333V5.41667H5.41667V20.5833H20.5833V15.1667H22.75Z" fill={fill} stroke={stroke} strokeWidth={strokeWidth} />
            <path d="M22.75 7.58333H18.4166V3.25H16.25V7.58333H11.9166V9.75H16.25V14.0833H18.4166V9.75H22.75V7.58333Z" fill={fill} stroke={stroke} strokeWidth={strokeWidth} />
        </svg>
    );
}
