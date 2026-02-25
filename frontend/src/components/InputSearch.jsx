import SearchIcon from './icons/Search';

export default function InputSearch({ placeholder = "Buscar elemento" }) {
    return (
        <div className="inputSearch">
            <input type="text" placeholder={placeholder} />
            <div className='searchbutton'>
                <SearchIcon width={20} height={20} />
            </div>
        </div>
    );
}
